package mutex.suzuki_kasami;

import app.AppConfig;
import app.Broadcast;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.mutex.SuzukiKasamiRequestTokenMessage;
import servent.message.mutex.SuzukiKasamiSendTokenMessage;
import servent.message.util.MessageUtil;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static app.ChordState.CHORD_SIZE;

public class SuzukiKasamiMutex implements Mutex {

    // RN[i] je najveci broj poruka koje je cvor i primio do sad preko request poruke od nas
    public List<Integer> RN = new CopyOnWriteArrayList<>();

    private SuzukiKasamiToken token = null; // null ako nema token
    private final AtomicBoolean hasToken = new AtomicBoolean(false); // true ako token nije null, da bi bilo lakse za get i te gluposti
    private final AtomicBoolean inCriticalSection = new AtomicBoolean(false); // da li smo trenutno u kriticnoj sekciji

    // Priority-based locking mechanism
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true); // fair ordering
    private final Semaphore urgentSemaphore = new Semaphore(1, true); // For urgent operations
    private final AtomicInteger waitingUrgentRequests = new AtomicInteger(0);
    private volatile boolean urgentOperationInProgress = false;

    // Lock acquisition queue with priority support
    private final PriorityBlockingQueue<LockRequest> lockRequestQueue = new PriorityBlockingQueue<>();
    private final AtomicInteger requestIdCounter = new AtomicInteger(0);

    // Inner class for lock requests with priority
    private static class LockRequest implements Comparable<LockRequest> {
        final int requestId;
        final boolean isUrgent;
        final long timestamp;
        final CountDownLatch completionLatch;

        LockRequest(boolean isUrgent) {
            this.requestId = System.identityHashCode(this);
            this.isUrgent = isUrgent;
            this.timestamp = System.nanoTime();
            this.completionLatch = new CountDownLatch(1);
        }

        @Override
        public int compareTo(LockRequest other) {
            // Urgent requests have higher priority
            if (this.isUrgent != other.isUrgent) {
                return this.isUrgent ? -1 : 1;
            }
            // Among same priority, earlier timestamp wins
            return Long.compare(this.timestamp, other.timestamp);
        }
    }

    public SuzukiKasamiMutex() {
        for (int i = 0; i < CHORD_SIZE; i++) {
            RN.add(0);
        }
    }

    public void lock() {
        lock(false); // Normal priority lock
    }

    public void urgentLock() {
        lock(true); // High priority lock
    }

    private void lock(boolean isUrgent) {
        LockRequest request = new LockRequest(isUrgent);

        if (isUrgent) {
            waitingUrgentRequests.incrementAndGet();
            urgentOperationInProgress = true;
        }

        try {
            // Acquire appropriate lock based on urgency
            if (isUrgent) {
                urgentSemaphore.acquire();
                readWriteLock.writeLock().lock();
            } else {
                // Wait for urgent operations to complete
                while (urgentOperationInProgress && waitingUrgentRequests.get() > 0) {
                    Thread.sleep(10);
                }
                readWriteLock.readLock().lock();
            }

            AppConfig.timestampedStandardPrint("Waiting for lock" + (isUrgent ? " (URGENT)" : ""));

            // ako nema token, a zeli da udje u kriticku sekciju
            if (token == null) {
                // inkrementiraj broj requestova
                int myId = AppConfig.myServentInfo.getChordId();
                RN.set(myId, RN.get(myId) + 1);

                int newVal = RN.get(myId);

                // broadcastuj
                BasicMessage newMsg = new SuzukiKasamiRequestTokenMessage(
                        AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.myServentInfo.getListenerPort(),
                        newVal
                );

                // Mark urgent requests in the message if needed
                if (isUrgent && newMsg instanceof SuzukiKasamiRequestTokenMessage) {
                    // You might want to add an urgent flag to your message class
                    // ((SuzukiKasamiRequestTokenMessage) newMsg).setUrgent(true);
                }

                Broadcast.broadcastMessage(newMsg);
            }

            // cekaj dok ne dobijes
            while (!hasToken.get() && AppConfig.isAlive.get()) {
                try {
                    Thread.sleep(isUrgent ? 50 : 100); // Urgent requests check more frequently
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // udji u kriticnu sekciju
            inCriticalSection.set(true);

            AppConfig.timestampedStandardPrint("Acquired lock" + (isUrgent ? " (URGENT)" : ""));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock acquisition interrupted", e);
        } finally {
            if (isUrgent) {
                waitingUrgentRequests.decrementAndGet();
                if (waitingUrgentRequests.get() == 0) {
                    urgentOperationInProgress = false;
                }
            }
        }
    }

    /**
     * <ul>
     * <li> When a site Si wants to enter the critical section and it does not have the token then it increments its sequence number RNi[i] and sends a request message REQUEST(i, sn) to all other sites in order to request the token.
     * Here sn is update value of RNi[i]
     * </ul>
     */
    public void lock(Set<Integer> broadcastToPorts) {
        lock(broadcastToPorts, false);
    }

    public void urgentLock(Set<Integer> broadcastToPorts) {
        lock(broadcastToPorts, true);
    }

    private void lock(Set<Integer> broadcastToPorts, boolean isUrgent) {
        if (isUrgent) {
            waitingUrgentRequests.incrementAndGet();
            urgentOperationInProgress = true;
        }

        try {
            // Acquire appropriate lock based on urgency
            if (isUrgent) {
                urgentSemaphore.acquire();
                readWriteLock.writeLock().lock();
            } else {
                // Wait for urgent operations to complete
                while (urgentOperationInProgress && waitingUrgentRequests.get() > 0) {
                    Thread.sleep(10);
                }
                readWriteLock.readLock().lock();
            }

            AppConfig.timestampedStandardPrint("Waiting for lock" + (isUrgent ? " (URGENT)" : ""));

            // ako nema token, a zeli da udje u kriticku sekciju
            if (token == null) {
                // inkrementiraj broj requestova
                int myId = AppConfig.myServentInfo.getChordId();
                RN.set(myId, RN.get(myId) + 1);

                int newVal = RN.get(myId);

                for (Integer nb : broadcastToPorts) {
                    Message newMsg = new SuzukiKasamiRequestTokenMessage(
                            AppConfig.myServentInfo.getListenerPort(),
                            nb,
                            newVal
                    );
                    // prosledi poruku dalje jer nemamo token
                    MessageUtil.sendMessage(newMsg);
                }
            }

            // cekaj dok ne dobijes
            while (!hasToken.get() && AppConfig.isAlive.get()) {
                try {
                    Thread.sleep(isUrgent ? 50 : 100); // Urgent requests check more frequently
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // udji u kriticnu sekciju
            inCriticalSection.set(true);

            AppConfig.timestampedStandardPrint("Acquired lock" + (isUrgent ? " (URGENT)" : ""));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock acquisition interrupted", e);
        } finally {
            if (isUrgent) {
                waitingUrgentRequests.decrementAndGet();
                if (waitingUrgentRequests.get() == 0) {
                    urgentOperationInProgress = false;
                }
            }
        }
    }

    /**
     * <ul>
     * <li> sets LN[i] = RNi[i] to indicate that its critical section request RNi[i] has been executed
     * <li> For every site Sj, whose ID is not present in the token queue Q, it appends its ID to Q if RNi[j] = LN[j] + 1
     *     to indicate that site Sj has an outstanding request
     * <li> After above updation, if the Queue Q is non-empty, it pops a site ID from the Q and sends the
     *    token to site indicated by popped ID.
     * <li> If the queue Q is empty, it keeps the token
     * </ul>
     */
    public void unlock() {
        boolean wasUrgent = urgentSemaphore.availablePermits() == 0;

        try {
            // sigurica ako pozovemo slucajno vise puta unlock
            if (token == null) {
                inCriticalSection.set(false);
                AppConfig.chordState.mutex.setToken(null);
                return;
            }

            // azuriraj LN[i]
            int myId = AppConfig.myServentInfo.getChordId();
            token.LN.set(myId, RN.get(myId));

            // za svaki node Sj ciji id nije prisutan u Q appenduj njegov id na Q ako RNi[j] = LN[j] + 1
            for (int i = 0; i < CHORD_SIZE; i++) {
                Integer nodeIPort = AppConfig.chordState.getPortOfNode(i);

                if (nodeIPort != null) {
                    if (!token.Q.contains(nodeIPort) && RN.get(i) == token.LN.get(i) + 1) {
                        token.Q.add(nodeIPort);
                    }
                }
            }

            // posalji token onom koji je cekao na token
            checkQueue();
            inCriticalSection.set(false);

        } finally {
            // Release the appropriate locks
            if (wasUrgent) {
                readWriteLock.writeLock().unlock();
                urgentSemaphore.release();
            } else {
                readWriteLock.readLock().unlock();
            }
        }

        AppConfig.timestampedStandardPrint("Unlocked");
    }

    public void checkQueue() {
        // This method can be called by both urgent and normal operations
        // so we need to be careful about locking
        boolean needsLock = !readWriteLock.isWriteLockedByCurrentThread() &&
                readWriteLock.getReadHoldCount() == 0;

        if (needsLock) {
            readWriteLock.readLock().lock();
        }

        try {
            // uzmi poslednjeg sa queue i salji mu token
            if (!token.Q.isEmpty() && this.hasToken.get()) {
                int sendTo = token.Q.poll();

                Message tokenMessage = new SuzukiKasamiSendTokenMessage(
                        AppConfig.myServentInfo.getListenerPort(),
                        sendTo,
                        token
                );

                MessageUtil.sendMessage(tokenMessage);
                AppConfig.chordState.mutex.setToken(null);
            }
        } finally {
            if (needsLock) {
                readWriteLock.readLock().unlock();
            }
        }
    }

    // Additional method to handle urgent token requests from other nodes
    public void handleUrgentTokenRequest(Message request) {
        // This could be called when receiving an urgent token request
        // You might want to prioritize sending the token to urgent requests

        try {
            urgentSemaphore.acquire();
            readWriteLock.writeLock().lock();

            // Handle the urgent request with higher priority
            // This is where you'd implement urgent request handling logic

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            readWriteLock.writeLock().unlock();
            urgentSemaphore.release();
        }
    }

    // region GETTERS AND SETTERS

    public boolean hasToken() {
        return hasToken.get();
    }

    public boolean inCritialSection() {
        return inCriticalSection.get();
    }

    public void setInCriticalSection(boolean value) {
        this.inCriticalSection.set(value);
    }

    public void setToken(SuzukiKasamiToken token) {
        if (token != null) {
            AppConfig.timestampedStandardPrint("=================JA IMAM TOKEN SADA==================");
            this.token = token;
            this.hasToken.set(true);
        } else {
            AppConfig.timestampedStandardPrint("=================NEMAM VISE TOKEN==================");
            this.token = null;
            this.hasToken.set(false);
            this.inCriticalSection.set(false);
        }
    }

    public SuzukiKasamiToken getToken() {
        return token;
    }

    // Additional utility methods
    public boolean hasUrgentOperationInProgress() {
        return urgentOperationInProgress;
    }

    public int getWaitingUrgentRequestsCount() {
        return waitingUrgentRequests.get();
    }

    //endregion
}