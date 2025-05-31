package mutex.suzuki_kasami;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static app.ChordState.CHORD_SIZE;

public class SuzukiKasamiToken implements Serializable {

    // LN[i] je broj requestova koji su skoro izvrseni od strane node-a i
    public List<Integer> LN = new CopyOnWriteArrayList<>();

    // cuva portove node-ova koji cekaju na token
    public Queue<Integer> Q = new LinkedList<>();

    public SuzukiKasamiToken() {
        for (int i = 0; i < CHORD_SIZE; i++) {
            LN.add(0);
        }
    }
}
