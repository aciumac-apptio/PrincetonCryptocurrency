import java.security.PublicKey;
import java.util.*;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    private UTXOPool utxoPool;

    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = utxoPool;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and  DONE
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS

        int i = 0;
        double inputSum = 0;
        Set<UTXO> claimedUtxos = new HashSet<>();

        for (Transaction.Input input : tx.getInputs()) {

            // all outputs claimed by {@code tx} are in the current UTXO pool
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            // If claimed multiple times
            if (claimedUtxos.contains(utxo)) {
                return false;
            }

            claimedUtxos.add(utxo);

            if (!utxoPool.contains(utxo)) {
                return false;
            }

            //verify signature
            if (!Crypto.verifySignature(utxoPool.getTxOutput(utxo).address, tx.getRawDataToSign(i), input.signature)) {
                return false;
            }

            inputSum += utxoPool.getTxOutput(utxo).value;
            i++;
        }

        double totalOutputValue = 0;
        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) {
                return false;
            }
            totalOutputValue += output.value;
        }

        if (totalOutputValue > inputSum) {
            return false;
        }


        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        Set<Transaction> acceptedTxs = new HashSet<>();
        Set<Transaction> validTxs = new HashSet<>();
        Set<Transaction> invalidTxs = new HashSet<>();

        // Process all valid transactions
        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                processValidTransaction(acceptedTxs, validTxs, tx);

            } else {
                invalidTxs.add(tx);
            }
        }

        // Process initially invalid transactions
        do {
            validTxs.clear();
            for (Transaction tx : invalidTxs) {
                if (isValidTx(tx)) {
                    processValidTransaction(acceptedTxs, validTxs, tx);
                }
            }
            invalidTxs.removeAll(validTxs);
        } while(!validTxs.isEmpty());

        Transaction[] arrayOfAcceptedTxs = acceptedTxs.toArray(new Transaction[acceptedTxs.size()]);
        return arrayOfAcceptedTxs;
    }

    private void processValidTransaction(Set<Transaction> acceptedTxs, Set<Transaction> validTxs, Transaction tx) {
        // Remove inputs from the pool
        for (Transaction.Input input : tx.getInputs()) {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            if (utxoPool.contains(utxo)) {
                utxoPool.removeUTXO(utxo);
            }
        }

        //Add new UTXO's to the pool
        List<Transaction.Output> outputList = tx.getOutputs();
        for (int i = 0; i < outputList.size(); i++) {
            UTXO utxo = new UTXO(tx.getHash(), i);

            utxoPool.addUTXO(utxo, outputList.get(i));
        }

        validTxs.add(tx);
        acceptedTxs.add(tx);
    }
}
