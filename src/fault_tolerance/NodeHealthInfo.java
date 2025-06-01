package fault_tolerance;

public class NodeHealthInfo {

    // vreme poslednje ping poruke
    private volatile long timestamp;

    // da li je gotovo broadcastovanje poruke koja se salje ako nema odg
    private volatile boolean finishedBroadcasting;

    // da li smo zivi
    private volatile NodeStatus nodeStatus;

    public NodeHealthInfo() {
        this.timestamp = System.currentTimeMillis();
        finishedBroadcasting = false;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isFinishedBroadcasting() {
        return finishedBroadcasting;
    }

    public void setFinishedBroadcasting(boolean finishedBroadcasting) {
        this.finishedBroadcasting = finishedBroadcasting;
    }

    public NodeStatus getNodeStatus() {
        return nodeStatus;
    }

    public void setNodeStatus(NodeStatus nodeStatus) {
        this.nodeStatus = nodeStatus;
    }
}
