package protocol;


import crypto.ConsistentHashing;
import p2p.NetworkInterface;
import p2p.NodeInterface;

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

    // small helper class to store a finger in a fingertable consisting of start, interval and the nodeId
    public class FingerTableEntry {
        int start;
        int intervalEnd;
        int nodeId;

        public FingerTableEntry(int start, int intervalEnd, int nodeId) {
            this.start = start;
            this.intervalEnd = intervalEnd;
            this.nodeId = nodeId;
        }
    }

    public void buildFingerTable() {
        
        // iterate thru each node in the ring
        for(Map.Entry<Integer, NodeInterface> entry : this.ring.entrySet()){

            int nodeId = entry.getKey();
            NodeInterface node = entry.getValue();
            TreeMap<Integer, FingerTableEntry> fingerTable = new TreeMap<>();

            // give the ith node m fingers
            for(int i = 1; i <= m; i++){

                int start = (nodeId + (1 << (i - 1))) % (1 << m);

                int intervalEnd = (nodeId + (1 << i)) % (1 << m);

                // get the succesor node closest to start
                Map.Entry<Integer, NodeInterface> finger = this.ring.ceilingEntry(start);

                // if the there are no more nodes, make first node the finger
                if (finger == null) {
                    finger = this.ring.firstEntry();
                }

                FingerTableEntry fingerTableEntry = new FingerTableEntry(start, intervalEnd, finger.getKey());

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
    public LookUpResponse lookUp(int keyIndex){
        /*
        implement this logic
         */
        return null;
    }



}
