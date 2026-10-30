package protocol;

import crypto.ConsistentHashing;
import p2p.NetworkInterface;
import p2p.NodeInterface;

import java.util.*;

/**
 * Chord protocol implementation, run on top of the custom simulator.
 */
public class ChordProtocol implements Protocol{

    // number of bits in an identifier, so the ring has 2^m positions
    public int m;

    // the network the protocol runs on
    public NetworkInterface network;

    // consistent hashing object, used to give each node its index
    public ConsistentHashing ch;

    // key name -> key index
    public HashMap<String, Integer> keyIndexes;

    // node id -> node, sorted so it can be walked as the ring
    private TreeMap<Integer, NodeInterface> ring = new TreeMap<>();

    public ChordProtocol(int m){
        this.m = m;
        setHashFunction();
        this.keyIndexes = new HashMap<String, Integer>();
    }

    /**
     * sets the hash function
     */
    public void setHashFunction(){
        this.ch = new ConsistentHashing(this.m);
    }

    /**
     * sets the network
     * @param network the network object
     */
    public void setNetwork(NetworkInterface network){
        this.network = network;
    }

    /**
     * sets the key indexes. They are used to test the lookup operation.
     * @param keyIndexes indexes of the keys
     */
    public void setKeys(HashMap<String, Integer> keyIndexes){
        this.keyIndexes = keyIndexes;
    }

    /**
     * @return the network object
     */
    public NetworkInterface getNetwork(){
        return this.network;
    }

    /**
     * Hashes every node onto the ring and links each node to its successor.
     * Throws an exception if two nodes get the same id.
     */
    public void buildOverlayNetwork(){

        // start with an empty ring in case the method runs more than once
        this.ring.clear();
        LinkedHashMap<String, NodeInterface> nodes = network.getTopology();

        // give every node its index and place it on the ring
        for (Map.Entry<String, NodeInterface> entry : nodes.entrySet()){

            String nodeName = entry.getKey();
            NodeInterface node = entry.getValue();

            int nodeId = ch.hash(nodeName);

            if(ring.containsKey(nodeId)){
                throw new IllegalStateException("Hash Collision for node ID: " + nodeId);
            }

            node.setId(nodeId);
            ring.put(nodeId, node);
        }

        // link every node to the next node on the ring
        for (Map.Entry<Integer, NodeInterface> entry : ring.entrySet()) {

            int currentId = entry.getKey();
            NodeInterface currentNode = entry.getValue();

            Map.Entry<Integer, NodeInterface> next =
                ring.higherEntry(currentId);

            // the last node wraps around to the first
            if (next == null) {
                next = ring.firstEntry();
            }

            NodeInterface successor = next.getValue();

            currentNode.addNeighbor(successor.getName(), successor);
        }
    }

    /**
     * One row of a finger table. The interval [start, intervalEnd] is inclusive and may wrap past 0,
     * e.g. (7,3) with m = 3.
     */
    public static class FingerTableEntry {
        int i;
        int start;
        int intervalEnd;
        // the node this row points to, the name is kept for printing
        int nodeId;
        String nodeName;

        public FingerTableEntry(int i, int start, int intervalEnd, int nodeId, String nodeName) {
            this.i = i;
            this.start = start;
            this.intervalEnd = intervalEnd;
            this.nodeId = nodeId;
            this.nodeName = nodeName;
        }

        // checks if an id, e.g. a key index, is inside the interval of this row
        public boolean contains(int id) {
            if (start <= intervalEnd) {
                return start <= id && id <= intervalEnd;
            }
            // the interval wraps past 0
            return id >= start || id <= intervalEnd;
        }

        // prints the row in the same style as the tables in the assignment
        @Override
        public String toString() {
            return "i=" + i + "  start=" + start + "  interval=(" + start + "," + intervalEnd + ")"
                    + "  successor=" + nodeName + " (index " + nodeId + ")";
        }
    }

    /**
     * A node's finger table, keyed by row number i. toString() is overridden because Node.print()
     * prints the routing table directly.
     */
    public static class FingerTable extends TreeMap<Integer, FingerTableEntry> {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            // one row per line, below the node line
            for (FingerTableEntry row : values()) {
                sb.append(System.lineSeparator()).append("    ").append(row);
            }
            return sb.toString();
        }
    }

    /**
     * Builds a finger table with m rows for every node on the ring. For node n, row i (i = 1,...,m) has
     * 1) start - (n + 2^(i-1)) mod 2^m
     * 2) interval - from start up to one before the next row's start. The last row wraps back to n.
     * 3) node - the first node at or after start
     * Together the m intervals cover the whole ring exactly once.
     */
    public void buildFingerTable() {

        // 2^m, the number of ids on the ring. All calculations are done mod this number.
        int ringSize = 1 << m;

        // build one finger table for each node on the ring
        for(Map.Entry<Integer, NodeInterface> entry : this.ring.entrySet()){

            int nodeId = entry.getKey();
            NodeInterface node = entry.getValue();
            FingerTable fingerTable = new FingerTable();

            for(int i = 1; i <= m; i++){

                // start = (n + 2^(i-1)) mod 2^m
                int start = (nodeId + (1 << (i - 1))) % ringSize;

                // the last row's "next start" is the first row's start, n + 1, so its interval ends at n
                int nextStart = (i < m) ? (nodeId + (1 << i)) % ringSize : (nodeId + 1) % ringSize;
                int intervalEnd = (nextStart - 1 + ringSize) % ringSize;

                // the first node at or after start, wrapping around to the first node on the ring
                Map.Entry<Integer, NodeInterface> finger = this.ring.ceilingEntry(start);
                if (finger == null) {
                    finger = this.ring.firstEntry();
                }

                FingerTableEntry fingerTableEntry = new FingerTableEntry(
                        i, start, intervalEnd, finger.getKey(), finger.getValue().getName());

                fingerTable.put(i, fingerTableEntry);
            }
            node.setRoutingTable(fingerTable);
        }
    }

    // the simulator has already stored each key index on the node responsible for it
    private boolean storesKey(NodeInterface node, int keyIndex) {
        Object data = node.getData();
        return (data instanceof Collection) && ((Collection<?>) data).contains(keyIndex);
    }

    /**
     * Looks up a key by following finger tables from a fixed start node. At each node: if it stores the
     * key we are done, otherwise jump to the node of the finger whose interval contains the key index.
     *
     * Example from the assignment (start at Node 4, key index 4):
     *   Node 4 -> row (3,4) -> Node 3 -> row (4,4) -> Node 1 stores the key. hop count = 3
     *
     * @param keyIndex index of the key
     * @return the visited nodes in order, and the node that stores the key
     */
    public LookUpResponse lookUp(int keyIndex){

        // any start node works, as long as it is the same for every key
        NodeInterface currentNode = ring.firstEntry().getValue();
        LookUpResponse lookUpResponse = new LookUpResponse(new LinkedHashSet<String>(), keyIndex, null);

        while (true){

            // add the current node to the route. A LinkedHashSet keeps the order of the visited nodes.
            lookUpResponse.peers_looked_up.add(currentNode.getName());

            // the current node stores the key, so we are done
            if (storesKey(currentNode, keyIndex)) {
                lookUpResponse.node_index = currentNode.getId();
                lookUpResponse.node_name = currentNode.getName();
                return lookUpResponse;
            }

            // getRoutingTable() returns an Object, so it is cast back to a FingerTable
            FingerTable fingerTable = (FingerTable) currentNode.getRoutingTable();

            // the intervals cover the whole ring, so exactly one row matches
            NodeInterface nextNode = null;
            for (FingerTableEntry finger : fingerTable.values()) {
                if (finger.contains(keyIndex)) {
                    nextNode = ring.get(finger.nodeId);
                    break;
                }
            }

            // should never happen on a correct ring, but guards against an infinite loop
            if (nextNode == null || lookUpResponse.peers_looked_up.contains(nextNode.getName())) {
                throw new IllegalStateException("Lookup of key index " + keyIndex + " is stuck at "
                        + currentNode.getName() + ", route so far: " + lookUpResponse.peers_looked_up);
            }

            currentNode = nextNode;
        }
    }
}
