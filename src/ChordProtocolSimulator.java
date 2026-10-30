import p2p.Network;
import p2p.NetworkInterface;
import p2p.NodeInterface;
import crypto.ConsistentHashing;
import protocol.ChordProtocol;
import protocol.LookUpResponse;
import protocol.Protocol;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Sets up a Chord network, assigns keys to nodes, and writes the network and lookup results to files.
 */
public class ChordProtocolSimulator {

    // number of bits in an identifier
    public int m;
    // number of nodes in the network
    public int nodeCount;
    // number of keys used in the simulation
    public int keyCount;

    // key name -> key index, e.g. ("key 1", 123). Key names are unique.
    public LinkedHashMap<String, Integer> keyIndexes;

    // the protocol that is simulated
    public Protocol protocol;

    // the network of nodes
    public NetworkInterface network;

    // used to calculate the indexes of nodes and keys
    public ConsistentHashing consistentHash;

    public ChordProtocolSimulator(Protocol protocol, Network network, int m, int nodeCount, int keyCount){
        this.keyIndexes = new LinkedHashMap<>();
        this.protocol = protocol;
        this.network = network;
        this.consistentHash = new ConsistentHashing(m);
        this.m = m;
        this.nodeCount = nodeCount;
        this.keyCount  = keyCount;
    }

    /**
     * Creates a new chord protocol simulator.
     *
     * @param network the network to run the protocol on
     * @param m number of bits in an identifier
     * @param keyCount number of keys to assign to nodes
     * @return the chord protocol simulator object
     */
    public static ChordProtocolSimulator getInstance(Network network, int m, int keyCount){

        Protocol chordProtocol = new ChordProtocol(m);
        int nodeCount = network.getSize();
        ChordProtocolSimulator chordProtocolSimulator = new ChordProtocolSimulator(chordProtocol, network, m, nodeCount,
                keyCount);
        return chordProtocolSimulator;
    }

    /**
     * Generates the keys and stores each key index on the node responsible for it.
     */
    public void assignKeys(){

        generateKeys();
        for(Map.Entry<String, Integer> entry: keyIndexes.entrySet())
        {
            // find the node responsible for the key and store the key index there
            int keyIndex =  entry.getValue();
            String peerName = findPeer(keyIndex);
            protocol.getNetwork().getTopology().get(peerName).addData(keyIndex);
        }
    }

    /**
     * Creates keys named "key 1" .. "key keyCount" and hashes each one to its index.
     */
    public  void generateKeys(){

        for(int i=1;i<keyCount+1;i++)
        {
            String keyName =  "key "+i;
            int keyIndex = consistentHash.hash(keyName);
            this.keyIndexes.put(keyName, keyIndex);
        }
    }

    /**
     * Finds the node responsible for a key: the first node whose index is at or after the key index
     * on the ring.
     *
     * @param key_index index of the key
     * @return the name of the responsible node, or null if none was found
     */
    public String findPeer(int key_index){

        List<Integer> sortedIndexes = new ArrayList<Integer>();
        int peerIndex = -1;

        // calculate the index of every node
        for(Map.Entry<String, NodeInterface> node: this.network.getTopology().entrySet()) {
            String nodeName = node.getValue().getName();
            int nodeIndex = consistentHash.hash(nodeName);
            sortedIndexes.add(nodeIndex);
        }

        Collections.sort(sortedIndexes);

        // past the largest node index, the key wraps around to the first node
        if(key_index> sortedIndexes.get(sortedIndexes.size()-1)){
            peerIndex = sortedIndexes.get(0);
        }
        else{
            // otherwise choose the first node with an index at or above the key index
            for(int i=0;i < sortedIndexes.size(); i++){
                if(sortedIndexes.get(i) >= key_index){
                    peerIndex = sortedIndexes.get(i);
                    break;
                }
            }
        }

        // no node was found. This should not happen if everything works.
        if (peerIndex==-1){
            return null;
        }
        return findPeerName(peerIndex);
    }

    /**
     * Maps a node index back to its name. Relies on nodes being named "Node 1", "Node 2", ...
     *
     * @param peerIndex index of the node
     * @return name of the node, or null if no node has that index
     */
    public String findPeerName(int peerIndex){

        int peerCount = nodeCount;
        for(int i=1; i<peerCount+1; i++){
            String name = "Node "+i;
            int index = consistentHash.hash(name);
            if(peerIndex==index){
                return name;
            }

        }
        return null;
    }

    /**
     * Prints every node with its neighbors, routing table and data.
     */
    public void printNetwork(){

        NetworkInterface network = protocol.getNetwork();
        network.printTopology();
    }

    /**
     * Prints the ring by following successors from the first node until it comes back around.
     */
    public void printRing(){
        NodeInterface head = null;
        for(Map.Entry<String, NodeInterface> node: this.network.getTopology().entrySet()){
            head = node.getValue();
            break;
        }
        System.out.println("........printing ring..............");

        if(head.getNeighbors().size()==0){
            return;
        }

        System.out.print(head.getName());
        NodeInterface next = head.getSuccessor();
        while(true){
            System.out.print(" --- "+next.getName());
            next  = next.getSuccessor();
            if(next.getName().equals(head.getName())){
                System.out.print(" --- "+next.getName()+"\n");
                break;
            }
        }
        System.out.println(".....................................");
    }

    /**
     * Looks up every key and prints whether the returned node actually stores it. Stops at the first failure.
     */
    public void testLookUp(){

        for(Map.Entry<String, Integer> entry: keyIndexes.entrySet())
        {

            LookUpResponse response = protocol.lookUp(entry.getValue());

            if(response == null)
            {
                return;
            }

            System.out.println(response.toString());
            // check whether the returned node really stores the key
            if(checkResponse(entry.getValue(),response.node_name)){
                System.out.println("lookup successful for "+entry.getKey());
            }
            else
            {
                System.out.println("lookup failed for "+entry.getKey());
                break;
            }
        }

    }

    /**
     * Looks up every key and writes one line per key to output/configuration_<node count>_<m>_.txt.
     * If a lookup ends at the wrong node, an ERROR line is written below it.
     * The last line in the file is the average hop count over all keys.
     */
    public void doLookUpAndGenerateOutputFiles(){
        int totalHops = 0;
        int successfulLookups = 0;

        File outputFolder = new File("output");
        outputFolder.mkdirs();

        String fileName = ("configuration_" + nodeCount + "_" + m + "_.txt");
        File outputFile = new File(outputFolder, fileName);

        try (FileWriter myWriter = new FileWriter(outputFile)) {

            for (Map.Entry<String, Integer> entry : keyIndexes.entrySet()) {

                // look up the key, starting from the same node every time
                LookUpResponse response = protocol.lookUp(entry.getValue());

                if (response == null) {
                    return;
                }

                // wrong lookups are still written and counted in the average
                totalHops += response.peers_looked_up.size();
                myWriter.write(
                    entry.getKey() + ":" + entry.getValue() +
                    response.toString() + "\n"
                );

                // check that the node found really stores the key
                if (checkResponse(entry.getValue(), response.node_name)) {
                    successfulLookups++;
                } else {
                    myWriter.write("  -> ERROR: " + response.node_name + " does not store " + entry.getKey() + "\n");
                }
            }

            // average over all keys. With 0 keys the average is 0.
            double averageHops = keyIndexes.isEmpty() ? 0.0 : (double) totalHops / keyIndexes.size();
            myWriter.write("average hop count = " + averageHops + "\n");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Checks whether a node actually stores the given key index.
     *
     * @param keyIndex index of the key
     * @param peerName name of the node
     * @return true if the node stores the key index, otherwise false
     */
    public boolean checkResponse(int keyIndex, String peerName){
        @SuppressWarnings("unchecked")
        LinkedHashSet<Integer> dataItems = (LinkedHashSet<Integer>) this.network.getNode(peerName).getData();
        for(Integer data : dataItems){
            if(data==keyIndex){
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the network, assigns keys, then builds the ring and the finger tables.
     */
    public void buildProtocol(){
        protocol.setNetwork(network);
        assignKeys();
        protocol.setKeys(keyIndexes);
        protocol.buildOverlayNetwork();
        protocol.buildFingerTable();
    }

    /**
     * Runs the simulation: builds the protocol, writes the network to a file, then writes the lookup results.
     */
    public void start(){

        // build the ring and the finger tables
        buildProtocol();
        // write the ring and the network to a file
        printNetworkToFile();
        // look up all keys and write the results and the average hop count to a file
        doLookUpAndGenerateOutputFiles();
    }

    /**
     * Writes the ring and the full topology to output/network_<node count>_<m>_.txt. The print methods
     * use System.out, so it is redirected to the file while printing and restored afterwards.
     */
    public void printNetworkToFile(){

        File outputFolder = new File("output");
        outputFolder.mkdirs();
        File networkFile = new File(outputFolder, "network_" + nodeCount + "_" + m + "_.txt");

        PrintStream console = System.out;
        try (PrintStream filePrinter = new PrintStream(networkFile)) {
            System.setOut(filePrinter);
            printRing();
            printNetwork();
            filePrinter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.setOut(console);
        }
    }

}
