import p2p.Network;
import protocol.ChordProtocol;
import protocol.Protocol;

/**
 * Starting point of the program. Builds the p2p network and runs the chord protocol simulator on it.
 * Run it with two arguments, the number of nodes and m.
 */
public class Simulator{

    /**
     * Creates the network with the given number of nodes.
     *
     * @param name name of the network
     * @param nodeCount number of nodes in the network
     * @return the network object
     */
    public Network createNetwork(String name, int nodeCount){
        Network network = Network.createNetwork(name, nodeCount);
        return network;
    }

    /**
     * Starts the simulation by creating the network and running the chord protocol simulator on it.
     *
     * @param args args[0] is the number of nodes, args[1] is m (number of bits in an identifier)
     */
    public void start(String[] args){

        // number of nodes in the network
        int nodeCount = Integer.parseInt(args[0]);

        // length of the indexes generated with consistent hashing
        int m = Integer.parseInt(args[1]);

        Network myNetwork = createNetwork("test network", nodeCount);

        // the number of keys depends on the size of the network
        int keyCount=0;
        if((nodeCount/2)<m)
        {
            keyCount=(int) nodeCount/2;
        }
        else
        {
            keyCount = nodeCount*3;
        }

        // create the chord protocol simulator and start it
        ChordProtocolSimulator chordProtocolSimulator = ChordProtocolSimulator.getInstance(myNetwork, m, keyCount);

        chordProtocolSimulator.start();
    }

    /**
     * Starting point of the simulator.
     *
     * @param args node count and m
     */
    public static void main(String[] args){
        Simulator simulator = new Simulator();
        simulator.start(args);
    }

}
