package p2p;

import java.util.*;

/**
 * A named set of peer nodes.
 */
public class Network implements NetworkInterface{

    // name of the network
    public String networkName;

    // node name -> node, in insertion order
    public LinkedHashMap<String, NodeInterface> network;

    public Network(String name){
        this.networkName = name;
        this.network = new LinkedHashMap<String, NodeInterface>();
    }

    public LinkedHashMap<String, NodeInterface> getTopology(){
        return this.network;
    }

    public NodeInterface getNode(String name){
        return this.network.get(name);
    }

    public void addNode(String name, NodeInterface node){
        this.network.put(name, node);
    }

    public void removeNode(String name){
        this.network.remove(name);
    }

    // prints every node in the network
    public void printTopology(){
        System.out.println("..............Printing network topology..............");
        for(Map.Entry<String, NodeInterface> nodeEntry: this.network.entrySet()){
            Node node = (Node) nodeEntry.getValue();
            node.print();
        }
        System.out.println("......................................................");
    }


    /**
     * Creates a network with nodes named "Node 1" .. "Node nodeCount".
     *
     * @param networkName name of the network
     * @param nodeCount number of nodes in the network
     * @return the network object
     */
    public static Network createNetwork(String networkName, int nodeCount){
        Network network = new Network(networkName);
        for(int i=1; i<nodeCount+1; i++){
            String nodeName = "Node "+i;
            Node node = new Node(nodeName);
            network.addNode(nodeName, node);
        }
        return network;
    }

    public int getSize(){
        return this.network.size();
    }
}
