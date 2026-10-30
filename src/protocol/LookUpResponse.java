package protocol;

import java.util.LinkedHashSet;

/**
 * Result of a lookup: the nodes visited in order, and the node that stores the key.
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
     * Formats the response as in the assignment. The simulator puts the key name and index in front, so a line
     * looks like: key 1:4 Node 1:4 hop count: 3, route: Node 4 Node 3 Node 1
     * The hop count includes the start node.
     */
    @Override
    public String toString(){
        StringBuilder result = new StringBuilder();
        result.append(" ").append(node_name).append(":").append(node_index);
        result.append(" hop count: ").append(peers_looked_up.size()).append(",");
        result.append(" route: ").append(String.join(" ", peers_looked_up));
        return result.toString();
    }
}
