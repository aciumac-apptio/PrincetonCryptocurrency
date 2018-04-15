import org.junit.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import static org.junit.Assert.assertTrue;

public class MaxFeeTxHandlerTest {
    KeyPair pk_scrooge;
    KeyPair pk_alice;
    KeyPair pk_bob;
    Main.Tx tx;
    UTXOPool utxoPool;

    private void InitializeWithSingleOutput() throws NoSuchAlgorithmException, SignatureException {
        this.pk_scrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        this.pk_alice = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        this.pk_bob = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        /*
         * Set up the root transaction:
         *
         * Generating a root transaction tx out of thin air, so that Scrooge owns a coin of value 10
         * By thin air I mean that this tx will not be validated, I just need it to get
         * a proper Transaction.Output which I then can put in the UTXOPool, which will be passed
         * to the TXHandler.
         */
        tx = new Main.Tx();
        tx.addOutput(10, pk_scrooge.getPublic());

        // This value has no meaning, but tx.getRawDataToSign(0) will access it in prevTxHash;
        byte[] initialHash = BigInteger.valueOf(0).toByteArray();
        tx.addInput(initialHash, 0);
        tx.signTx(pk_scrooge.getPrivate(), 0);

        /*Set up the UTXOPool
        The transaction output of the root transaction is the initial unspent output.*/
        this.utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(tx.getHash(), 0);
        utxoPool.addUTXO(utxo, tx.getOutput(0));
    }

    @Test
    public void twoTransactionsClaimSameOutput_ChooseMaxFee() throws NoSuchAlgorithmException, SignatureException {
        InitializeWithSingleOutput();

        //Set up a test Transaction
        Main.Tx tx2 = new Main.Tx();

        // the Transaction.Output of tx at position 0 has a value of 10
        tx2.addInput(tx.getHash(), 0);
        tx2.addOutput(7, pk_alice.getPublic());
        tx2.addOutput(3, pk_alice.getPublic());
        tx2.signTx(pk_scrooge.getPrivate(), 0);

        //Set up a test Transaction
        Main.Tx tx3 = new Main.Tx();
        tx3.addInput(tx.getHash(), 0);
        tx3.addOutput(7, pk_bob.getPublic());
        tx3.signTx(pk_scrooge.getPrivate(), 0);

        /*
         * Start the test
         * Remember that the utxoPool contains a single unspent Transaction.Output which is the coin from Scrooge.
         */
        MaxFeeTxHandler maxFeeTxHandler = new MaxFeeTxHandler(utxoPool);
        Transaction[] acceptedTransactions = maxFeeTxHandler.handleTxs(new Transaction[]{tx2, tx3});

        assertTrue("One valid transaction", acceptedTransactions.length == 1);
        assertTrue("",acceptedTransactions[0].getOutput(0) == pk_bob.getPublic());
    }

}
