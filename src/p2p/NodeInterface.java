package p2p;

import java.util.Collection;

public interface NodeInterface {
    /**
     * @return the name of the node
     */
    String getName();

    /**
     * @return the node id (in Chord, its position on the ring)
     */
    int getId();

    /**
     * sets the id of the node
     * @param id node id
     */
    void setId(int id);

    /**
     * returns the neighbor with the given name
     * @param name name of the neighbor
     * @return the neighbor node
     */
    NodeInterface getNeighbor(String name);

    /**
     * adds a new neighbor to the node
     * @param name name of the neighbor
     * @param node the neighbor node
     */
    void addNeighbor(String name, NodeInterface node);

    /**
     * @return all the neighbors of the node
     */
    Collection<NodeInterface> getNeighbors();

    /**
     * Stores a protocol-specific routing table, e.g. a Chord finger table. It should override toString()
     * so it shows up in print().
     */
    void setRoutingTable(Object obj);

    /**
     * @return the routing table, which the protocol casts back to its own type
     */
    Object getRoutingTable();

    /**
     * @return the first neighbor that was added to this node
     */
    NodeInterface getSuccessor();

    /**
     * Stores a protocol-specific data item, e.g. a key index in Chord. It should override toString()
     * so it shows up in print().
     */
    void addData(Object data);

    /**
     * @return the stored data, which the protocol casts back to its own type
     */
    Object getData();

    /**
     * Prints the node's name, id, neighbors, routing table and data.
     */
    void print();
}
