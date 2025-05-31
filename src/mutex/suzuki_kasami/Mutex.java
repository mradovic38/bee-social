package mutex.suzuki_kasami;

import java.util.Set;

public interface Mutex {
    void lock(Set<Integer> broadcastToPorts);
    void unlock();
}
