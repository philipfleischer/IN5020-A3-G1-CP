package protocol;

import p2p.NetworkInterface;

import java.util.HashMap;

public interface Protocol {

    /**
     * sets the network
     * @param network the network object
     */
    public void setNetwork(NetworkInterface network);

    /**
     * @return the network object
     */
    public NetworkInterface getNetwork();

    /**
     * sets the key indexes. They are used to test the lookup operation.
     * @param keyIndexes indexes of the keys
     */
    public void setKeys(HashMap<String, Integer> keyIndexes);

    /**
     * Gives every node an id and links each node to its successor on the ring.
     * The network must be set first.
     */
    public void buildOverlayNetwork();

    /**
     * Builds the routing table (finger table in Chord) for every node.
     */
    public void buildFingerTable();

    /**
     * Routes a lookup for the key through the network.
     *
     * @param keyIndex index of the key
     * @return the visited nodes in order, and the node that stores the key
     */
    public LookUpResponse lookUp(int keyIndex);
}
