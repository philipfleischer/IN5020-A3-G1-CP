package crypto;


import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Maps strings to positions on a ring of size 2^m, using MD5 mod 2^m.
 */
public class ConsistentHashing {

    // number of bits in the hash value
    public int m;

    /**
     * @param m length of the hash value in bits
     */
    public ConsistentHashing(int m){
        this.m=m;
    }

    /**
     * Calculates the hash value for the data.
     *
     * @param data the string to hash
     * @return a position on the ring, in [0, 2^m)
     */
    public int hash(String data){
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        // MD5 digest of the data as a byte array
        byte[] messageDigest = md.digest(data.getBytes());

        // signum 1 so the digest is read as a positive number
        BigInteger res = new BigInteger(1, messageDigest);

        // consistent hashing, hash mod 2^m
        BigInteger length = BigInteger.valueOf((long) Math.pow(2, m));
        res = res.mod(length);

        return res.intValue();

    }

    // prints the ids of four nodes with m = 3
    public static void test1(){
        ConsistentHashing ch = new ConsistentHashing(3);
        String nodes[] ={"Node 1", "Node 2", "Node 3", "Node 4"};
        for(int i=0; i< nodes.length; i++)
            System.out.println( ch.hash(nodes[i]));

    }

    // hashes 1000 nodes with m = 20 and prints how many distinct ids they got
    public static void test2(){
        ConsistentHashing ch = new ConsistentHashing(20);
        int length = 1000;
        String nodes[] = new String[length];
        Set<Integer> set = new HashSet<Integer>();
        ArrayList<Integer> res = new ArrayList<Integer>();

        for (int i = 0; i < length; i++) {
            nodes[i] = "Node " + (i + 1);
        }

        for (int i = 0; i < length; i++) {
            res.add(ch.hash(nodes[i]));
            set.add(ch.hash(nodes[i]));
        }

        Collections.sort(res);
        System.out.println(res);
        System.out.println(set.size());
    }
}
