package protocol;


import crypto.ConsistentHashing;
import p2p.NetworkInterface;
import p2p.NodeInterface;

import java.io.FileNotFoundException;
import java.util.*;

/**
 * This class implements the chord protocol. The protocol is tested using the custom built simulator.
 */
public class ChordProtocol implements Protocol{

    // length of the identifier that is used for consistent hashing
    public int m;

    // network object
    public NetworkInterface network;

    // consisent hasing object
    public ConsistentHashing ch;

    // key indexes. tuples of (<key name>, <key index>)
    public HashMap<String, Integer> keyIndexes;

    // using a sorted map structure to represent the "ring"
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
     * sets the key indexes. Those key indexes can be used to  test the lookup operation.
     * @param keyIndexes - indexes of keys
     */
    public void setKeys(HashMap<String, Integer> keyIndexes){
        this.keyIndexes = keyIndexes;
    }



    /**
     *
     * @return the network object
     */
    public NetworkInterface getNetwork(){
        return this.network;
    }






    /**
     * This method builds the overlay network.  It assumes the network object has already been set. It generates indexes
     *     for all the nodes in the network. Based on the indexes it constructs the ring and places nodes on the ring.
     *         algorithm:
     *           1) for each node:
     *           2)     find neighbor based on consistent hash (neighbor should be next to the current node in the ring)
     *           3)     add neighbor to the peer (uses Peer.addNeighbor() method)
     */
    public void buildOverlayNetwork(){

        // if the method runs multiple times we start with a cleared ring
        this.ring.clear();

        // get all the nodes from the network into a local copy "nodes"
        LinkedHashMap<String, NodeInterface> nodes = network.getTopology();

        // go thru each created node, give them ids and add to ring topology
        for (Map.Entry<String, NodeInterface> entry : nodes.entrySet()){

            String nodeName = entry.getKey();
            NodeInterface node = entry.getValue();

            int nodeId = ch.hash(nodeName);

            if(ring.containsKey(nodeId)){
                throw new IllegalStateException(
                    "Hash Collision for node ID: " + nodeId
                );
            }

            node.setId(nodeId);
            ring.put(nodeId, node);
        }

        // add a successor neighbor to each ring node
        for (Map.Entry<Integer, NodeInterface> entry : ring.entrySet()) {

            int currentId = entry.getKey();
            NodeInterface currentNode = entry.getValue();

            Map.Entry<Integer, NodeInterface> next =
                ring.higherEntry(currentId);

            if (next == null) {
                next = ring.firstEntry();
            }

            NodeInterface successor = next.getValue();

            currentNode.addNeighbor(successor.getName(), successor);
        }

    }




    /**
     * This method builds the finger table. The finger table is the routing table used in the chord protocol to perform
     * lookup operations. The finger table stores m-entries. Each ith entry points to the ith finger of the node.
     * Each ith entry stores the information of it's neighbor that is responsible for indexes ((n+2^i-1) mod 2^m).
     * i = 1,...,m.
     *
     *Each finger table entry should consists of
     *     1) start value - (n+2^i-1) mod 2^m. i = 1,...,m
     *     2) interval - [finger[i].start, finger[i+1].start)
     *     3) node - first node in the ring that is responsible for indexes in the interval
     */

    // small helper class to store one finger (one row) in a fingertable: row number i, start, interval and the node
    public static class FingerTableEntry {
        int i;
        int start;
        // the interval is [start, intervalEnd], both ends included. It can wrap around 0, e.g. (7,3) with m = 3
        int intervalEnd;
        int nodeId;
        // we keep the name too, so the table can be printed like in the assignment
        String nodeName;

        public FingerTableEntry(int i, int start, int intervalEnd, int nodeId, String nodeName) {
            this.i = i;
            this.start = start;
            this.intervalEnd = intervalEnd;
            this.nodeId = nodeId;
            this.nodeName = nodeName;
        }

        // checks if an id (e.g. a key index) is inside the interval of this row.
        // The lookup uses this to find "the successor whose interval contains the index of the key".
        public boolean contains(int id) {
            if (start <= intervalEnd) {
                // normal interval, e.g. (3,4): id must be between the two ends
                return start <= id && id <= intervalEnd;
            }
            // wrapped interval, e.g. (7,3): it passes 0, so id is either at the end or at the start of the ring
            return id >= start || id <= intervalEnd;
        }

        // prints the row in the same style as the tables in the assignment
        @Override
        public String toString() {
            return "i=" + i + "  start=" + start + "  interval=(" + start + "," + intervalEnd + ")"
                    + "  successor=" + nodeName + " (index " + nodeId + ")";
        }
    }

    // the finger table of one node: row number i -> row. It is still a TreeMap, so the lookup can use it as before.
    // We only override toString(), because Node.print() calls routingTable.toString() when the network is printed.
    // Without this, the output only shows object addresses like "ChordProtocol$FingerTableEntry@3941a79c".
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

    public void buildFingerTable() {

        // 2^m, the number of ids on the ring. All calculations are done mod this number.
        int ringSize = 1 << m;

        // iterate thru each node in the ring
        for(Map.Entry<Integer, NodeInterface> entry : this.ring.entrySet()){

            int nodeId = entry.getKey();
            NodeInterface node = entry.getValue();
            FingerTable fingerTable = new FingerTable();

            // give the node m fingers, i = 1..m
            for(int i = 1; i <= m; i++){

                // start = (n + 2^(i-1)) mod 2^m
                int start = (nodeId + (1 << (i - 1))) % ringSize;

                // the interval ends one before the start of the next row: (n + 2^i) - 1.
                // For the last row the "next row" is the first row, whose start is n + 1, so the interval ends at n.
                // This way the m intervals together cover the whole ring exactly once (e.g. Node 3: (4,4) (5,6) (7,3)).
                int nextStart = (i < m) ? (nodeId + (1 << i)) % ringSize : (nodeId + 1) % ringSize;
                int intervalEnd = (nextStart - 1 + ringSize) % ringSize;

                // get the succesor node closest to start
                Map.Entry<Integer, NodeInterface> finger = this.ring.ceilingEntry(start);

                // if the there are no more nodes, make first node the finger
                if (finger == null) {
                    finger = this.ring.firstEntry();
                }

                FingerTableEntry fingerTableEntry = new FingerTableEntry(
                        i, start, intervalEnd, finger.getKey(), finger.getValue().getName());

                // add finger to the fingertable
                fingerTable.put(i, fingerTableEntry);
            }

            node.setRoutingTable(fingerTable);

        }

    }



    /**
     * This method performs the lookup operation.
     *  Given the key index, it starts with one of the node in the network and follows through the finger table.
     *  The correct successors would be identified and the request would be checked in their finger tables successively.
     *   Finally the request will reach the node that contains the data item.
     *
     * @param keyIndex index of the key
     * @return names of nodes that have been searched and the final node that contains the key
     */

    // helper function to check if a node really stores the key. The simulator has already put every key index in the
    // data of the node that is responsible for it, so we just look there.
    private boolean storesKey(NodeInterface node, int keyIndex) {
        Object data = node.getData();
        // getData() returns an Object, but the simulator stores the key indexes in a collection of Integers
        return (data instanceof Collection) && ((Collection<?>) data).contains(keyIndex);
    }

    // The lookup, as described in the assignment:
    //   1) start at the same node for every key
    //   2) the current node has now seen the lookup, so it is added to the route (this is one hop)
    //   3) if the current node stores the key, we are done
    //   4) if not, find the row in its finger table whose interval contains the key index, and go to that row's node
    // Example from the assignment (start at Node 4, key 1 with index 4):
    //   Node 4 -> row (3,4) -> Node 3 -> row (4,4) -> Node 1 stores the key. hop count = 3
    public LookUpResponse lookUp(int keyIndex){

        // start point (the node with the smallest index). Any node is allowed, but it must be the same for every key
        NodeInterface currentNode = ring.firstEntry().getValue();
        LookUpResponse lookUpResponse = new LookUpResponse(new LinkedHashSet<String>(), keyIndex, null);

        while (true){

            // Register the node we are checking. A LinkedHashSet keeps the order, so this is the route
            lookUpResponse.peers_looked_up.add(currentNode.getName());

            // the current node stores the key -> return its information
            if (storesKey(currentNode, keyIndex)) {
                lookUpResponse.node_index = currentNode.getId();
                lookUpResponse.node_name = currentNode.getName();
                return lookUpResponse;
            }

            // check fingertable for the next node jump (needed casting from type Object)
            FingerTable fingerTable = (FingerTable) currentNode.getRoutingTable();

            // the next node is the successor of the row whose interval contains the key index.
            // The m intervals cover the whole ring, so exactly one row matches.
            NodeInterface nextNode = null;
            for (FingerTableEntry finger : fingerTable.values()) {
                if (finger.contains(keyIndex)) {
                    nextNode = ring.get(finger.nodeId);
                    break;
                }
            }

            // safety net: in a correct ring this never happens, but if it did we would loop forever.
            // So we stop with a clear error message instead.
            if (nextNode == null || lookUpResponse.peers_looked_up.contains(nextNode.getName())) {
                throw new IllegalStateException("Lookup of key index " + keyIndex + " is stuck at "
                        + currentNode.getName() + ", route so far: " + lookUpResponse.peers_looked_up);
            }

            currentNode = nextNode;
        }
    }
}
