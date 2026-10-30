package p2p;

import java.util.LinkedHashMap;

public interface NetworkInterface {
    /**
     * @return all nodes, keyed by node name
     */
    LinkedHashMap<String, NodeInterface> getTopology();

    /**
     * returns the node with the given name
     * @param name name of the node
     * @return node object
     */
    NodeInterface getNode(String name);

    /**
     * adds a new node to the network
     * @param name name of the node
     * @param node the node object
     */
    void addNode(String name, NodeInterface node);

    /**
     * removes a node from the network
     * @param name name of the node to remove
     */
    void removeNode(String name);

    /**
     * Prints every node in the network.
     */
    void printTopology();

    /**
     * @return number of nodes
     */
    int getSize();
}
