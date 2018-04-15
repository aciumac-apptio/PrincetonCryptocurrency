// Copyright (C) 2018 Artiom Ciumac
/*
 * Considerable amount of code was taken from main test code for Cousera cryptocurrency assignment1
 * Based on code by
 * - Sven Mentl
 * - Pietro Brunetti
 * - Bruce Arden
 * - Tero Keski-Valkama
 */

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.List;

/**
 * Test 1: test handleTx() with a single valid transaction with a single input and two outputs
 * Test 2: test handleTx() with a single valid transaction with two inputs and a single output
 * Test 3: test handleTx() with valid transactions in order
 * Test 4: test handleTx() with valid transactions in reverse order
 * Test 5: test handleTx() with first case from scenario described here
 *          https://www.coursera.org/learn/cryptocurrency/discussions/weeks/1/threads/EDvHOgJnEeiJowoNU7sPSA
 * Test 6: test handleTx() with second case from scenario described here
 *          https://www.coursera.org/learn/cryptocurrency/discussions/weeks/1/threads/EDvHOgJnEeiJowoNU7sPSA
 *
 */

public class HandleTxTest {
    KeyPair pk_scrooge;
    KeyPair pk_alice;
    KeyPair pk_bob;
    KeyPair pk_james;
    UTXOPool utxoPool;
    Main.Tx tx;

    // Adds two coins to the UTXO pool and builds key pairs
    public void InitializeWithSingleOutput() throws NoSuchAlgorithmException, SignatureException {

        // Generate key pairs for accounts
        this.pk_scrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        this.pk_alice = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        this.pk_bob = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        this.pk_james = KeyPairGenerator.getInstance("RSA").generateKeyPair();
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

    // Adds two coins to the UTXO pool and builds key pairs
    public void InitializeWithTwoOutputs() throws SignatureException, NoSuchAlgorithmException {

        // Generate key pairs for accounts
        this.pk_scrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        this.pk_alice = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        this.pk_bob = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        this.pk_james = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        /*
         * Set up the root transaction:         *
         * Scroodge owns two coins, valued at 10 and 5
         */
        this.tx = new Main.Tx();
        tx.addOutput(10, pk_scrooge.getPublic());
        tx.addOutput(5, pk_scrooge.getPublic());

        // This value has no meaning, but tx.getRawDataToSign(0) will access it in prevTxHash;
        byte[] initialHash = BigInteger.valueOf(0).toByteArray();
        tx.addInput(initialHash, 0);
        tx.signTx(pk_scrooge.getPrivate(), 0);

        /*
         * Set up the UTXOPool
         * Two transaction outputs of the root transaction is the initial unspent output.
         */
        this.utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(tx.getHash(), 0);
        UTXO utxo_1 = new UTXO(tx.getHash(), 1);
        utxoPool.addUTXO(utxo, tx.getOutput(0));
        utxoPool.addUTXO(utxo_1, tx.getOutput(1));
    }

    // Test 1: test handleTx() with a single valid transaction with a single input and two outputs
    @Test
    public void testHandleTxSingleValidTransactionSingleInputTwoOutputs() throws NoSuchAlgorithmException, SignatureException {

        InitializeWithSingleOutput();

        //Set up a test Transaction
        Main.Tx tx2 = new Main.Tx();

        // the Transaction.Output of tx at position 0 has a value of 10
        tx2.addInput(tx.getHash(), 0);

        // I split the coin of value 10 into 2 coins and sent them to Alice (same address)
        tx2.addOutput(7, pk_alice.getPublic());
        tx2.addOutput(3, pk_alice.getPublic());

        // Note that in the real world fixed-point types would be used for the values, not doubles.
        // Doubles exhibit floating-point rounding errors. This type should be for example BigInteger
        // and denote the smallest coin fractions (Satoshi in Bitcoin).

        // There is only one (at position 0) Transaction.Input in tx2
        // and it contains the coin from Scrooge, therefore I have to sign with the private key from Scrooge
        tx2.signTx(pk_scrooge.getPrivate(), 0);

        /*
         * Start the test
         * Remember that the utxoPool contains a single unspent Transaction.Output which is the coin from Scrooge.
         */
        TxHandler txHandler = new TxHandler(utxoPool);
        assertTrue("One valid transaction", txHandler.handleTxs(new Transaction[]{tx2}).length == 1);
        assertTrue("Two UTXO's are created", utxoPool.getAllUTXO().size() == 2);
    }

    // Test 2: test handleTx() with a single valid transaction with two inputs and a single output
    @Test
    public void testHandleTxSingleValidTransactionTwoInputsOneOutput() throws NoSuchAlgorithmException, SignatureException {
        InitializeWithTwoOutputs();

        Main.Tx tx2 = new Main.Tx();

        // the Transaction.Output of tx at position 0 has a value of 10
        tx2.addInput(tx.getHash(), 0);
        tx2.addInput(tx.getHash(), 1);

        // Both coins consumed to produce a single output
        tx2.addOutput(15, pk_alice.getPublic());

        // Have to sign both inputs
        tx2.signTx(pk_scrooge.getPrivate(), 0);
        tx2.signTx(pk_scrooge.getPrivate(), 1);

        // Start the test
        TxHandler txHandler = new TxHandler(utxoPool);
        Transaction[] acceptedTx = txHandler.handleTxs(new Transaction[]{tx2});
        assertTrue("One valid transaction", acceptedTx.length == 1);
        assertTrue("UTXO created", utxoPool.getAllUTXO().size() == 1);

        List<UTXO> utxos = utxoPool.getAllUTXO();

        assertTrue("Alice owns " + utxoPool.getTxOutput(utxos.get(0)).value + " coins. Scroodge paid "  + " coins ",
                utxoPool.contains(new UTXO(tx2.getHash(), 0)) && utxoPool.getTxOutput(utxos.get(0)).value
                        == tx.getOutput(0).value + tx.getOutput(1).value);
    }

    // Test 3: test handleTx() with valid transactions in order
    @Test
    public void testHandleTxTwoValidTransactionsInOrder() throws SignatureException, NoSuchAlgorithmException {
        InitializeWithSingleOutput();

        Main.Tx tx2 = new Main.Tx();

        // the Transaction.Output of tx at position 0 has a value of 10
        tx2.addInput(tx.getHash(), 0);

        // Scroodge gives 7 to Alice and 3 to Bob (Transaction 2)
        tx2.addOutput(7, pk_alice.getPublic());
        tx2.addOutput(3, pk_bob.getPublic());
        tx2.signTx(pk_scrooge.getPrivate(), 0);

        // Alice gives 7 to Bob (Transaction 3)
        Main.Tx tx3 = new Main.Tx();
        tx3.addInput(tx2.getHash(), 0);
        tx3.addOutput(7, pk_bob.getPublic());
        tx3.signTx(pk_alice.getPrivate(), 0);

        TxHandler txHandler = new TxHandler(utxoPool);
        Transaction[] acceptedTx = txHandler.handleTxs(new Transaction[]{tx2, tx3});

        //Assert
        assertTrue("Two transactions accepted.", acceptedTx.length == 2);
        assertTrue("UTXO pool has 2 utxos.", utxoPool.getAllUTXO().size() == 2);
        double totalValue = 0;
        for (UTXO utxo : utxoPool.getAllUTXO()) {
            assertTrue("Bob owns this", utxoPool.getTxOutput(utxo).address == pk_bob.getPublic());
            totalValue += utxoPool.getTxOutput(utxo).value;
        }
        assertTrue("Bob owns 10", totalValue == 10);
    }

    // Test 4: test handleTx() with valid transactions in reverse order
    @Test
    public void testHandleTxTwoValidTransactionsReverseOrder() throws SignatureException, NoSuchAlgorithmException {
        InitializeWithSingleOutput();

        Main.Tx tx2 = new Main.Tx();

        // the Transaction.Output of tx at position 0 has a value of 10
        tx2.addInput(tx.getHash(), 0);

        // Scroodge gives 7 to Alice and 3 to Bob (Transaction 2)
        tx2.addOutput(7, pk_alice.getPublic());
        tx2.addOutput(3, pk_bob.getPublic());
        tx2.signTx(pk_scrooge.getPrivate(), 0);

        // Alice gives 7 to Bob (Transaction 3)
        Main.Tx tx3 = new Main.Tx();
        tx3.addInput(tx2.getHash(), 0);
        tx3.addOutput(7, pk_bob.getPublic());
        tx3.signTx(pk_alice.getPrivate(), 0);

        TxHandler txHandler = new TxHandler(utxoPool);
        Transaction[] acceptedTx = txHandler.handleTxs(new Transaction[]{tx3, tx2});

        //Assert
        assertTrue("Two transactions accepted.", acceptedTx.length == 2);
        assertTrue("UTXO pool has 2 utxos.", utxoPool.getAllUTXO().size() == 2);
        double totalValue = 0;
        for (UTXO utxo : utxoPool.getAllUTXO()) {
            assertTrue("Bob owns this", utxoPool.getTxOutput(utxo).address == pk_bob.getPublic());
            totalValue += utxoPool.getTxOutput(utxo).value;
        }
        assertTrue("Bob owns 10", totalValue == 10);
    }

    // Test 5: test handleTx() with first case from scenario described here
    // https://www.coursera.org/learn/cryptocurrency/discussions/weeks/1/threads/EDvHOgJnEeiJowoNU7sPSA
    @Test
    public void testHandleTxThreeTransactionsInOrder() throws SignatureException, NoSuchAlgorithmException {
        InitializeWithTwoOutputs();

        // [A(owned by Scroodge), B(owned by Scroodge)] -> [C(owned by Alice)]
        Main.Tx tx1 = new Main.Tx();
        tx1.addInput(tx.getHash(), 0);
        tx1.addInput(tx.getHash(), 1);
        tx1.addOutput(15, pk_alice.getPublic());
        tx1.signTx(pk_scrooge.getPrivate(), 0);
        tx1.signTx(pk_scrooge.getPrivate(), 1);

        //[A(owned by Scroodge)] -> [D(owned by Bob)]
        Main.Tx tx2 = new Main.Tx();
        tx2.addInput(tx.getHash(), 0);
        tx2.addOutput(10, pk_bob.getPublic());
        tx2.signTx(pk_scrooge.getPrivate(), 0);

        //[B(owned by Scroodge)] - > [E(owned by James)]
        Main.Tx tx3 = new Main.Tx();
        tx3.addInput(tx.getHash(), 1);
        tx3.addOutput(5, pk_james.getPublic());
        tx3.signTx(pk_scrooge.getPrivate(), 0);

        TxHandler txHandler = new TxHandler(utxoPool);
        Transaction[] acceptedTx = txHandler.handleTxs(new Transaction[]{tx1, tx2, tx3});

        assertTrue("One transaction accepted.", acceptedTx.length == 1);
    }

    // Test 6: test handleTx() with second case from scenario described here
    // https://www.coursera.org/learn/cryptocurrency/discussions/weeks/1/threads/EDvHOgJnEeiJowoNU7sPSA
    @Test
    public void testHandleTxThreeTransactionsDifferentOrder() throws SignatureException, NoSuchAlgorithmException {
        InitializeWithTwoOutputs();

        // [A(owned by Scroodge), B(owned by Scroodge)] -> [C(owned by Alice)]
        Main.Tx tx1 = new Main.Tx();
        tx1.addInput(tx.getHash(), 0);
        tx1.addInput(tx.getHash(), 1);
        tx1.addOutput(15, pk_alice.getPublic());
        tx1.signTx(pk_scrooge.getPrivate(), 0);
        tx1.signTx(pk_scrooge.getPrivate(), 1);


        //[A(owned by Scroodge)] -> [D(owned by Bob)]
        Main.Tx tx2 = new Main.Tx();
        tx2.addInput(tx.getHash(), 0);
        tx2.addOutput(10, pk_bob.getPublic());
        tx2.signTx(pk_scrooge.getPrivate(), 0);


        //[B(owned by Scroodge)] - > [E(owned by James)]
        Main.Tx tx3 = new Main.Tx();
        tx3.addInput(tx.getHash(), 1);
        tx3.addOutput(5, pk_james.getPublic());
        tx3.signTx(pk_scrooge.getPrivate(), 0);

        TxHandler txHandler = new TxHandler(utxoPool);
        Transaction[] acceptedTx = txHandler.handleTxs(new Transaction[]{tx2, tx3, tx1});

        assertTrue("Two transactions accepted.", acceptedTx.length == 2);
    }

}
