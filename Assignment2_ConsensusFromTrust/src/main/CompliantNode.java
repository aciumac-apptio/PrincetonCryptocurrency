import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
    private boolean[] followees;

    private double p_graph;
    private double p_malicious;
    private double p_txDistribution;
    private int initialNumRounds;
    private int currentRound;

    private Set<Transaction> acceptedTransactions;
    //private HashMap<Transaction, Set<Integer>> consensusSet;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        // IMPLEMENT THIS
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.initialNumRounds = numRounds;
        this.currentRound = 0;

        //this.consensusSet = new HashMap<Transaction, Set<Integer>>();

    }

    public void setFollowees(boolean[] followees) {
        // IMPLEMENT THIS

        // Nodes to whom I send transactions
        this.followees = followees;
       /* int nodesFollow = 0;
        for (boolean b: followees) {
            if (b) {
                nodesFollow++;
            }
        }
        this.nodesToConfirmTx = (int) Math.floor(p_malicious * nodesFollow) + 1;*/
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        // IMPLEMENT THIS
//        for (Transaction tx: pendingTransactions) {
//            this.pendingTransactions.put(tx, 0);
//        }

        this.acceptedTransactions = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
        // IMPLEMENT THIS

        if (currentRound == 0) {
            Set<Transaction> copy = new HashSet<Transaction>(acceptedTransactions);
            acceptedTransactions.clear();
            return copy;
        }
        //return consensusSet.keySet();
        return acceptedTransactions;
    }


    public void receiveFromFollowees(Set<Candidate> candidates) {
        // IMPLEMENT THIS
        HashMap<Transaction, Set<Integer>> preProcessTx = new HashMap<Transaction, Set<Integer>>();
        boolean[] updatedFollowees = new boolean[followees.length];

        for (Candidate candidate : candidates) {
            updatedFollowees[candidate.sender] = true;
            Set<Integer> votes = new HashSet<Integer>();
/*            if (preProcessTx.containsKey(candidate.tx)) {
                votes = preProcessTx.get(candidate.tx);
            }
            votes.add(candidate.sender);
            preProcessTx.put(candidate.tx, votes);*/

            if (currentRound < initialNumRounds * 0.9) {
                acceptedTransactions.add(candidate.tx);
            } else if (preProcessTx.containsKey(candidate.tx)){
                votes = preProcessTx.get(candidate.tx);
                preProcessTx.put(candidate.tx, votes);
            } else {
                votes.add(candidate.sender);
                preProcessTx.put(candidate.tx, votes);
            }
        }


        for (Transaction tx: preProcessTx.keySet()) {
            if (preProcessTx.get(tx).size() >= Math.ceil(p_malicious * candidates.size())) {
                //consensusSet.put(tx, preProcessTx.get(tx));
                acceptedTransactions.add(tx);
            }
        }

        followees = updatedFollowees;
        currentRound++;
    }

/*    private boolean areEqualCandidates(Candidate one, Candidate two) {
        return one.tx.equals(two.tx) && one.sender == two.sender;
    }*/
}
