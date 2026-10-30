package p2p;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * A peer in the network, with a name, an id, neighbors, a routing table and stored data.
 */
public class Node implements NodeInterface{

    // name of the node
    public String name;

    // position on the ring in Chord, -1 until assigned
    public int id;

    // neighbors of the node. In chord the only neighbor is the successor.
    public HashMap<String, NodeInterface> neighbors;

    // protocol-specific, e.g. the Chord finger table
    public Object routingTable;

    // protocol-specific, e.g. the key indexes this node is responsible for in Chord
    public LinkedHashSet<Object> data;

    public Node(String name) {
        this.id = -1;
        this.name = name;
        this.data = new LinkedHashSet<>();
        this.neighbors= new HashMap<String, NodeInterface>();
    }

    public String getName() {
        return this.name;
    }

    public void setId(int id){
        this.id = id;
    }

    public int getId(){
        return this.id;
    }

    public Object getData() {
        return this.data;
    }

    public void addData(Object data) {
        this.data.add(data);
    }

    public void removeData(Object data){
        this.data.remove(data);
    }

    public Collection<NodeInterface> getNeighbors(){
        return  this.neighbors.values();
    }

    public NodeInterface getNeighbor(String name){
        return this.neighbors.get(name);
    }

    public void addNeighbor(String name, NodeInterface node){
        this.neighbors.put(name, node);
    }

    public void removeNeighbor(String name){
        this.neighbors.remove(name);
    }

    public void setRoutingTable(Object obj){
        this.routingTable =obj;
    }

    public Object getRoutingTable(){
        return this.routingTable;
    }

    // the successor is the first neighbor that was added
    public NodeInterface getSuccessor(){
        return this.neighbors.entrySet().iterator().next().getValue();
    }

    // prints name, index and neighbors on one line, then the routing table and the data
    public void print(){
        System.out.print("Node : "+ this.getName());
        System.out.print("\tIndex: "+this.getId());
        Collection<NodeInterface> neightbors = this.getNeighbors();
        System.out.print("\tNeighbors: ");

        for (NodeInterface node : neightbors) {
            System.out.print(node.getName() + "\t");
        }

        // relies on the protocol's routing table overriding toString()
        if (routingTable != null) {
            System.out.print("\t" + this.routingTable.toString());
        }

        System.out.println();
        if(data!=null){
            System.out.println("Data : "+this.data.toString());
        }
    }
}
