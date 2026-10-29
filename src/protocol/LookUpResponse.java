package protocol;

import java.util.LinkedHashSet;

/**
 * This class prints the the response of the lookup. This is class prints the names of the nodes whose finger table
 * has been checked, the destination node index, it's name and hop count.
 */
public class LookUpResponse {
    public LinkedHashSet<String> peers_looked_up;
    public int node_index;
    public String node_name;

    public LookUpResponse(LinkedHashSet<String> peers_looked_up, int node_index,String node_name){
        this.peers_looked_up = peers_looked_up;
        this.node_index = node_index;
        this.node_name = node_name;
    }

    /**
     * Formats the response as in the assignment text. The simulator puts "<key name>:<key index>" in front, so one
     * line looks like:  key 1:4 Node 1:4 hop count: 3, route: Node 4 Node 3 Node 1
     * hop count is the number of nodes that have seen the lookup (including the start node), and the route lists
     * those nodes in the order they were visited.
     */
    @Override
    public String toString(){
        // StringBuilder + String.join: one pass, and no trailing space after the last node name
        StringBuilder result = new StringBuilder();
        result.append(" ").append(node_name).append(":").append(node_index);
        result.append(" hop count: ").append(peers_looked_up.size()).append(",");
        result.append(" route: ").append(String.join(" ", peers_looked_up));
        return result.toString();
    }
} 