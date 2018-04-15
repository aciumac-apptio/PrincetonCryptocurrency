/**
 * John Brown is the author of this document
 * https://www.coursera.org/learn/cryptocurrency/programming/1fi2s/blockchain/discussions/threads/IbED3tNnEeaBeg5U4yHl7A
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Test;

//import Block;
//import BlockChain;
//import BlockHandler;
//import Transaction;
//import UTXO;
//import UTXOPool;

import java.util.Arrays;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PublicKey;

public class DropBoxJunitTests {

    public int nPeople;
    public int nUTXOTx;
    public int maxUTXOTxOutput;
    public double maxValue;
    public int nTxPerTest;
    public int maxInput;
    public int maxOutput;

    public ArrayList<RSAKeyPair> people;

    public DropBoxJunitTests() throws FileNotFoundException, IOException {

        this.nPeople = 20;
        this.nUTXOTx = 20;
        this.maxUTXOTxOutput = 20;
        this.maxValue = 10;
        this.nTxPerTest = 50;
        this.maxInput = 4;
        this.maxOutput = 20;
        people = new ArrayList<RSAKeyPair>();

        byte[] key = new byte[32];
        for (int i = 0; i < 32; i++) {
            key[i] = (byte) 1;
        }

        people = new ArrayList<RSAKeyPair>();
        for (int i = 0; i < nPeople; i++)
            people.add(new RSAKeyPair());
    }

    @Test
    //Process a block with no transactions
    public void test1() {
        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
        block.finalize();

        assertTrue("Failed to process genesis block",blockHandler.processBlock(block));
    }

    @Test
    //Process a block with a single valid transaction
    public void test2() {
        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
        Transaction spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertTrue("Could not process block with 1 tx.",blockHandler.processBlock(block));
    }

    @Test
    //Process a block with many valid transactions
    public void test3() {

        for (int k = 0; k < 20; k++) {
            Block genesisBlock = new Block(null, people.get(0).getPublicKey());
            genesisBlock.finalize();

            BlockChain blockChain = new BlockChain(genesisBlock);
            BlockHandler blockHandler = new BlockHandler(blockChain);

            Block block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
            Transaction spendCoinbaseTx = new Transaction();
            spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);

            double totalValue = 0;
            UTXOPool utxoPool = new UTXOPool();
            int numOutputs = 0;
            HashMap<UTXO, RSAKeyPair> utxoToKeyPair = new HashMap<UTXO, RSAKeyPair>();
            HashMap<Integer, RSAKeyPair> keyPairAtIndex = new HashMap<Integer, RSAKeyPair>();

            for (int j = 0; j < maxUTXOTxOutput; j++) {
                int rIndex = SampleRandom.randomInt(people.size());
                PublicKey addr = people.get(rIndex).getPublicKey();
                double value = SampleRandom.randomDouble(maxValue);
                if (totalValue + value > Block.COINBASE)
                    break;
                spendCoinbaseTx.addOutput(value, addr);
                keyPairAtIndex.put(j, people.get(rIndex));
                totalValue += value;
                numOutputs++;
            }

            spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
            spendCoinbaseTx.finalize();
            block.addTransaction(spendCoinbaseTx);

            for (int j = 0; j < numOutputs; j++) {
                UTXO ut = new UTXO(spendCoinbaseTx.getHash(), j);
                utxoPool.addUTXO(ut, spendCoinbaseTx.getOutput(j));
                utxoToKeyPair.put(ut, keyPairAtIndex.get(j));
            }
            ArrayList<UTXO> utxoSet = utxoPool.getAllUTXO();
            HashSet<UTXO> utxosSeen = new HashSet<UTXO>();
            int maxValidInput = Math.min(maxInput, utxoSet.size());

            for (int i = 0; i < nTxPerTest; i++) {
                Transaction tx = new Transaction();
                HashMap<Integer, UTXO> utxoAtIndex = new HashMap<Integer, UTXO>();
                int nInput = SampleRandom.randomInt(maxValidInput) + 1;
                int numInputs = 0;
                double inputValue = 0;
                for (int j = 0; j < nInput; j++) {
                    UTXO utxo = utxoSet.get(SampleRandom.randomInt(utxoSet.size()));
                    if (!utxosSeen.add(utxo)) {
                        j--;
                        nInput--;
                        continue;
                    }
                    tx.addInput(utxo.getTxHash(), utxo.getIndex());
                    inputValue += utxoPool.getTxOutput(utxo).value;
                    utxoAtIndex.put(j, utxo);
                    numInputs++;
                }

                if (numInputs == 0)
                    continue;

                int nOutput = SampleRandom.randomInt(maxOutput) + 1;
                double outputValue = 0;
                for (int j = 0; j < nOutput; j++) {
                    double value = SampleRandom.randomDouble(maxValue);
                    if (outputValue + value > inputValue)
                        break;
                    int rIndex = SampleRandom.randomInt(people.size());
                    PublicKey addr = people.get(rIndex).getPublicKey();
                    tx.addOutput(value, addr);
                    outputValue += value;
                }
                for (int j = 0; j < numInputs; j++) {
                    tx.addSignature(utxoToKeyPair.get(utxoAtIndex.get(j)).sign(tx.getRawDataToSign(j)), j);
                    //tx.addSignature(utxoToKeyPair.get(utxoAtIndex.get(j)).sign(tx.getRawDataToSign(j)), j);
                }
                tx.finalize();
                block.addTransaction(tx);
            }

            block.finalize();
            String err = "Failed with many transactions. Pass: "+k;
            assertTrue(err,blockHandler.processBlock(block));
        }

    }

    @Test
    //Process a block with some double spends
    public void test14() {
        for (int k = 0; k < 20; k++) {
            Block genesisBlock = new Block(null, people.get(0).getPublicKey());
            genesisBlock.finalize();

            BlockChain blockChain = new BlockChain(genesisBlock);
            BlockHandler blockHandler = new BlockHandler(blockChain);

            Block block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
            Transaction spendCoinbaseTx = new Transaction();
            spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);

            double totalValue = 0;
            UTXOPool utxoPool = new UTXOPool();
            int numOutputs = 0;
            HashMap<UTXO, RSAKeyPair> utxoToKeyPair = new HashMap<UTXO, RSAKeyPair>();
            HashMap<Integer, RSAKeyPair> keyPairAtIndex = new HashMap<Integer, RSAKeyPair>();

            for (int j = 0; j < maxUTXOTxOutput; j++) {
                int rIndex = SampleRandom.randomInt(people.size());
                PublicKey addr = people.get(rIndex).getPublicKey();
                double value = SampleRandom.randomDouble(maxValue);
                if (totalValue + value > Block.COINBASE)
                    break;
                spendCoinbaseTx.addOutput(value, addr);
                keyPairAtIndex.put(j, people.get(rIndex));
                totalValue += value;
                numOutputs++;
            }

            spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
            spendCoinbaseTx.finalize();
            block.addTransaction(spendCoinbaseTx);

            for (int j = 0; j < numOutputs; j++) {
                UTXO ut = new UTXO(spendCoinbaseTx.getHash(), j);
                utxoPool.addUTXO(ut, spendCoinbaseTx.getOutput(j));
                utxoToKeyPair.put(ut, keyPairAtIndex.get(j));
            }

            ArrayList<UTXO> utxoSet = utxoPool.getAllUTXO();
            HashSet<UTXO> utxosSeen = new HashSet<UTXO>();
            int maxValidInput = Math.min(maxInput, utxoSet.size());

            boolean notCorrupted = true;

            for (int i = 0; i < nTxPerTest; i++) {
                Transaction tx = new Transaction();
                HashMap<Integer, UTXO> utxoAtIndex = new HashMap<Integer, UTXO>();
                int nInput = SampleRandom.randomInt(maxValidInput) + 1;
                int numInputs = 0;
                double inputValue = 0;
                for (int j = 0; j < nInput; j++) {
                    UTXO utxo = utxoSet.get(SampleRandom.randomInt(utxoSet.size()));
                    if (!utxosSeen.add(utxo)) {
                        notCorrupted = false;
                    }
                    tx.addInput(utxo.getTxHash(), utxo.getIndex());
                    inputValue += utxoPool.getTxOutput(utxo).value;
                    utxoAtIndex.put(j, utxo);
                    numInputs++;
                }

                if (numInputs == 0)
                    continue;

                int nOutput = SampleRandom.randomInt(maxOutput) + 1;
                double outputValue = 0;
                for (int j = 0; j < nOutput; j++) {
                    double value = SampleRandom.randomDouble(maxValue);
                    if (outputValue + value > inputValue)
                        break;
                    int rIndex = SampleRandom.randomInt(people.size());
                    PublicKey addr = people.get(rIndex).getPublicKey();
                    tx.addOutput(value, addr);
                    outputValue += value;
                }
                for (int j = 0; j < numInputs; j++) {
                    tx.addSignature(utxoToKeyPair.get(utxoAtIndex.get(j)).sign(tx.getRawDataToSign(j)), j);
                }
                tx.finalize();
                block.addTransaction(tx);
            }

            block.finalize();
            String message = String.format("(%d) Failed double spend check for %scorrupt block",
                    k, notCorrupted ? "non-" : "");
            assertTrue(message,blockHandler.processBlock(block) == notCorrupted);
        }
    }

    @Test
    //Process a new genesis block
    public void test4() {

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block genesisblock = new Block(null, people.get(1).getPublicKey());
        genesisblock.finalize();

        assertFalse("Failed to not add second genesis block",blockHandler.processBlock(genesisblock));
    }

    @Test
    //Process a block with an invalid prevBlockHash
    public void test5() {
        System.out.println("");

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        byte[] hash = genesisBlock.getHash();
        byte[] hashCopy = Arrays.copyOf(hash, hash.length);
        hashCopy[0]++;
        Block block = new Block(hashCopy, people.get(1).getPublicKey());
        block.finalize();

        assertFalse("Invalid prevBlockHash not detected.",blockHandler.processBlock(block));
    }

    @Test
    //Process blocks with different sorts of invalid transactions
    public void test6() {
        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

/*        Block block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
        Transaction spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        byte[] rawData = spendCoinbaseTx.getRawDataToSign(0);
        rawData[0]++;
        spendCoinbaseTx.addSignature(people.get(0).sign(rawData), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertFalse("Shoud not add genesis block to blockchain",blockHandler.processBlock(block));*/

        Block block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
        Transaction spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(1).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();
        assertFalse("Bad COINBASE tx signature",blockHandler.processBlock(block));


        block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
        spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE + 1, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();
        assertFalse("Proposed tx overspends COINBASE",blockHandler.processBlock(block));


        block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
        spendCoinbaseTx = new Transaction();
        byte[] hash = genesisBlock.getCoinbase().getHash();
        byte[] hashCopy = Arrays.copyOf(hash, hash.length);
        hashCopy[0]++;
        spendCoinbaseTx.addInput(hashCopy, 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertFalse("Bad txHash",
                blockHandler.processBlock(block));

        block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
        spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 1);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertFalse("Coinbase tx has only 1 output",
                blockHandler.processBlock(block));

        block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
        spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertFalse("Failed to detect double spend within transaction.",
                blockHandler.processBlock(block));

        block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
        spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(-Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertFalse("Cannot add a negative output",
                blockHandler.processBlock(block));
    }

    @Test
    //Process multiple blocks directly on top of the genesis block
    public void test7() {
        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block block;
        Transaction spendCoinbaseTx;

        for (int i = 0; i < 100; i++) {
            block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
            spendCoinbaseTx = new Transaction();
            spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
            spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
            spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
            spendCoinbaseTx.finalize();
            block.addTransaction(spendCoinbaseTx);
            block.finalize();
            assertTrue("Failed to add "+i+"th block",
                    blockHandler.processBlock(block));
        }
    }

    @Test
    //Process a block containing a transaction that claims a UTXO already claimed by a transaction in its parent
    public void test15() {

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
        Transaction spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertTrue("Failed to add 1st block",
                blockHandler.processBlock(block));

        Block prevBlock = block;

        block = new Block(prevBlock.getHash(), people.get(2).getPublicKey());
        spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE - 1, people.get(2).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(1).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertFalse("Failed to detect previosly spent output",
                blockHandler.processBlock(block));
    }


    @Test
    //Process a block containing a transaction that claims a UTXO not on its branch
    public void test16() {
        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
        Transaction spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertTrue("Failed to add 1st block",
                blockHandler.processBlock(block));

        Block prevBlock = block;

        block = new Block(genesisBlock.getHash(), people.get(2).getPublicKey());
        spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(prevBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(2).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(1).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertFalse("Second block contains illegal spend",
                blockHandler.processBlock(block));
    }

    @Test
    //Process a block containing a transaction that claims
    //a UTXO from earlier in its branch that has not yet been claimed
    public void test17() {

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        assertTrue("Wrong # of UTXOs",
                blockChain.getMaxHeightUTXOPool().getAllUTXO().size()==1);
        UTXO utxo = new UTXO(genesisBlock.getCoinbase().getHash(),0);
        UTXO genCoinbase = utxo;
        assertTrue("UTXOpool does not have coinbase tx",blockChain.getMaxHeightUTXOPool().contains(utxo));

        Block block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
        Transaction spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertTrue("Failed to add first block",
                blockHandler.processBlock(block));
        assertTrue("Wrong # of UTXOs",
                blockChain.getMaxHeightUTXOPool().getAllUTXO().size()==2);

        Block prevBlock = block;
        Transaction retainTx = spendCoinbaseTx;

        UTXO b1Coinbase = new UTXO(block.getCoinbase().getHash(),0);
        assertTrue("Transaction output not in UTXOpool",blockChain.getMaxHeightUTXOPool().contains(b1Coinbase));
        assertFalse("Tx output not removed",blockChain.getMaxHeightUTXOPool().contains(genCoinbase));
        UTXO retain = new UTXO(retainTx.getHash(),0);
        assertTrue("Transaction output not in UTXOpool",blockChain.getMaxHeightUTXOPool().contains(retain));


        block = new Block(prevBlock.getHash(), people.get(2).getPublicKey());
        spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(prevBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(2).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(1).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertTrue("Failed to second block",
                blockHandler.processBlock(block));

        prevBlock = block;

        block = new Block(prevBlock.getHash(), people.get(3).getPublicKey());
        Transaction spendOldUTXOTransaction = new Transaction();
        spendOldUTXOTransaction.addInput(retainTx.getHash(), 0);
        spendOldUTXOTransaction.addOutput(Block.COINBASE, people.get(2).getPublicKey());
        spendOldUTXOTransaction.addSignature(people.get(1).sign(spendOldUTXOTransaction.getRawDataToSign(0)), 0);
        spendOldUTXOTransaction.finalize();
        block.addTransaction(spendOldUTXOTransaction);
        block.finalize();

        assertTrue("Failed to add third block",
                blockHandler.processBlock(block));
    }

    @Test
    //Process a linear chain of blocks
    public void test8() {
        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block block;
        Block prevBlock = genesisBlock;
        Transaction spendCoinbaseTx;

        for (int i = 0; i < 100; i++) {
            block = new Block(prevBlock.getHash(), people.get(0).getPublicKey());
            spendCoinbaseTx = new Transaction();
            spendCoinbaseTx.addInput(prevBlock.getCoinbase().getHash(), 0);
            spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
            spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
            spendCoinbaseTx.finalize();
            block.addTransaction(spendCoinbaseTx);
            block.finalize();
            prevBlock = block;

            assertTrue("Failed to add block "+i,
                    blockHandler.processBlock(block));
        }

    }


    @Test
    //Process a linear chain of blocks of length CUT_OFF_AGE
    //and then a block on top of the genesis block
    public void test9() {
        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block block;
        Block prevBlock = genesisBlock;
        Transaction spendCoinbaseTx;

        for (int i = 0; i < BlockChain.CUT_OFF_AGE; i++) {
            block = new Block(prevBlock.getHash(), people.get(0).getPublicKey());
            spendCoinbaseTx = new Transaction();
            spendCoinbaseTx.addInput(prevBlock.getCoinbase().getHash(), 0);
            spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
            spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
            spendCoinbaseTx.finalize();
            block.addTransaction(spendCoinbaseTx);
            block.finalize();
            prevBlock = block;

            assertTrue("Failed to add regular block "+i,
                    blockHandler.processBlock(block));
        }

        block = new Block(genesisBlock.getHash(), people.get(0).getPublicKey());
        block.finalize();

        assertTrue("Failed to add last block",
                blockHandler.processBlock(block));

    }

    @Test
    // Process a linear chain of blocks of length CUT_OFF_AGE + 1
    // and then a block on top of the genesis block.
    public void test10() {

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block block;
        Block prevBlock = genesisBlock;
        Transaction spendCoinbaseTx;

        for (int i = 0; i < BlockChain.CUT_OFF_AGE + 1; i++) {
            block = new Block(prevBlock.getHash(), people.get(0).getPublicKey());
            spendCoinbaseTx = new Transaction();
            spendCoinbaseTx.addInput(prevBlock.getCoinbase().getHash(), 0);
            spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
            spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
            spendCoinbaseTx.finalize();
            block.addTransaction(spendCoinbaseTx);
            block.finalize();
            prevBlock = block;

            assertTrue("Failed to add regular block "+i,
                    blockHandler.processBlock(block));
        }

        //assertFalse("Aged block not properly removed", blockChain.contains(genesisBlock));

        block = new Block(genesisBlock.getHash(), people.get(0).getPublicKey());
        block.finalize();

        assertFalse("Failed to drop aged block",
                blockHandler.processBlock(block));
    }

    @Test
    // Create a block when no transactions have been processed
    public void test11() {

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block createdBlock = blockHandler.createBlock(people.get(1).getPublicKey());
        assertTrue("Created block is null",createdBlock != null);
        assertTrue("Created block parent is not genesis block",
                createdBlock.getPrevBlockHash().equals(genesisBlock.getHash()));
        assertTrue("Failed empty block creation",
                createdBlock.getTransactions().size() == 0);
    }

    @Test
    // Create a block after a single valid transaction has been processed
    public void test12() {

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Transaction spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        blockHandler.processTx(spendCoinbaseTx);


        Transaction.Input input = spendCoinbaseTx.getInput(0);
        UTXO utxo = new UTXO(input.prevTxHash,input.outputIndex);
        //assertTrue("UTXOPool does not contain transaction",blockChain.getMaxHeightUTXOPool().contains(utxo));
        assertFalse("UTXOPool does not contain transaction",blockChain.getMaxHeightUTXOPool().contains(utxo));

        ArrayList<Transaction> txs = blockChain.getTransactionPool().getTransactions();
        Transaction tx = txs.get(0);
        input = tx.getInput(0);
        UTXO utxo2 = new UTXO(input.prevTxHash,input.outputIndex);
        assertTrue("Different utxos",utxo.equals(utxo2));
        //assertTrue("UTXOPool does not contain transaction",blockChain.getMaxHeightUTXOPool().contains(utxo2));
        assertFalse("UTXOPool does not contain transaction",blockChain.getMaxHeightUTXOPool().contains(utxo2));


        Block createdBlock = blockHandler.createBlock(people.get(1).getPublicKey());
        assertTrue("Created block is null",createdBlock != null  );
        assertTrue("Bad parent",createdBlock.getPrevBlockHash().equals(genesisBlock.getHash()) );
        assertTrue("# of transactions = "+createdBlock.getTransactions().size(),
                createdBlock.getTransactions().size() == 1);
        assertTrue("Wrong transaction",
                createdBlock.getTransaction(0).equals(spendCoinbaseTx));
    }



    @Test
    //Create a block after a valid transaction has been processed,
    //then create a second block
    public void test22() {

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Transaction spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        blockHandler.processTx(spendCoinbaseTx);

        Block createdBlock = blockHandler.createBlock(people.get(1).getPublicKey());
        Block createdBlock2 = blockHandler.createBlock(people.get(2).getPublicKey());

        assertTrue("Failed: Second block after single tx",
                createdBlock != null && createdBlock.getPrevBlockHash().equals(genesisBlock.getHash()) && createdBlock.getTransactions().size() == 1 && createdBlock.getTransaction(0).equals(spendCoinbaseTx) && createdBlock2 != null && createdBlock2.getPrevBlockHash().equals(createdBlock.getHash()) && createdBlock2.getTransactions().size() == 0);
    }

    @Test
    // Create a block after a valid transaction has been processed
    // that is already in a block in the longest valid branch
    public void test19() {

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());

        Transaction spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertTrue("Failed to add 1st block",
                blockHandler.processBlock(block));

        blockHandler.processTx(spendCoinbaseTx);
        Block createdBlock = blockHandler.createBlock(people.get(1).getPublicKey());

        assertTrue("Null created block",createdBlock != null);
        assertTrue("Created block parent is not 1st block",
                createdBlock.getPrevBlockHash().equals(block.getHash()) );
        assertTrue("Created block should not have transactions",createdBlock.getTransactions().size() == 0);
    }

    @Test
    //Create a block after a valid transaction has been processed
    //that uses a UTXO already claimed by a transaction
    //in the longest valid branch
    public void test20() {

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());

        Transaction spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertTrue("Failed to add 1st block",
                blockHandler.processBlock(block));

        Transaction spendCoinbaseTx2 = new Transaction();
        spendCoinbaseTx2.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx2.addOutput(Block.COINBASE - 1, people.get(1).getPublicKey());
        spendCoinbaseTx2.addSignature(people.get(0).sign(spendCoinbaseTx2.getRawDataToSign(0)), 0);
        spendCoinbaseTx2.finalize();

        blockHandler.processTx(spendCoinbaseTx2);
        Block createdBlock = blockHandler.createBlock(people.get(1).getPublicKey());

        assertTrue("Null created block", createdBlock != null);
        assertTrue("Created block has wrong parent",
                createdBlock.getPrevBlockHash().equals(block.getHash()) );
    }

    @Test
    // Create a block after a valid transaction has been processed
    // that is not a double spend on the longest valid branch and
    // has not yet been included in any other block"
    public void test21() {

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());

        Transaction spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE - 1, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        block.addTransaction(spendCoinbaseTx);
        block.finalize();

        assertTrue("Could not add 1st block",
                blockHandler.processBlock(block));

        Transaction spendPrevTx = new Transaction();
        spendPrevTx.addInput(block.getCoinbase().getHash(), 0);
        spendPrevTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendPrevTx.addSignature(people.get(1).sign(spendPrevTx.getRawDataToSign(0)), 0);
        spendPrevTx.finalize();

        blockHandler.processTx(spendPrevTx);
        Block createdBlock = blockHandler.createBlock(people.get(1).getPublicKey());

        assertTrue("Null created block", createdBlock != null);
        assertTrue("Created block has wrong parent",
                createdBlock.getPrevBlockHash().equals(block.getHash()) );
        assertTrue("Created block does not just 1 transaction",
                createdBlock.getTransactions().size() == 1);
        assertTrue("Created block has wrong transaction",
                createdBlock.getTransaction(0).equals(spendPrevTx));
    }

    @Test
    //Create a block after only invalid transactions have been processed
    public void test13() {

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Transaction spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE + 2, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        blockHandler.processTx(spendCoinbaseTx);

        Block createdBlock = blockHandler.createBlock(people.get(1).getPublicKey());
        assertTrue("Null created block", createdBlock != null);
        assertTrue("Created block has wrong parent",
                createdBlock.getPrevBlockHash().equals(genesisBlock.getHash()) );
        assertTrue("Created block has transactions",
                createdBlock.getTransactions().size() == 0);
    }

    @Test
    //Process a transaction, create a block, process a transaction,
    //create a block, ...
    public void test23() {

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Transaction spendCoinbaseTx;
        Block prevBlock = genesisBlock;

        for (int i = 0; i < 20; i++) {
            spendCoinbaseTx = new Transaction();
            spendCoinbaseTx.addInput(prevBlock.getCoinbase().getHash(), 0);
            spendCoinbaseTx.addOutput(Block.COINBASE, people.get(0).getPublicKey());
            spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
            spendCoinbaseTx.finalize();
            blockHandler.processTx(spendCoinbaseTx);

            Block createdBlock = blockHandler.createBlock(people.get(0).getPublicKey());
            String item = String.format("(%d) ", i);
            assertTrue(item+"Null created block", createdBlock != null);
            assertTrue(item+"Created block has wrong parent",
                    createdBlock.getPrevBlockHash().equals(prevBlock.getHash()));
            assertTrue(item+"Created block has "+createdBlock.getTransactions().size()+
                    " tranactions", createdBlock.getTransactions().size() == 1);
            assertTrue(item+"Wrong transaction",
                    createdBlock.getTransaction(0).equals(spendCoinbaseTx));
            prevBlock = createdBlock;
        }
    }

    @Test
    //Process a transaction, create a block,
    //then process a block on top of that block with a transaction
    //claiming a UTXO from that transaction
    public void test24() {

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Transaction spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        blockHandler.processTx(spendCoinbaseTx);

        Block createdBlock = blockHandler.createBlock(people.get(1).getPublicKey());

        Block newBlock = new Block(createdBlock.getHash(), people.get(1).getPublicKey());
        Transaction spendTx = new Transaction();
        spendTx.addInput(spendCoinbaseTx.getHash(), 0);
        spendTx.addOutput(Block.COINBASE, people.get(2).getPublicKey());
        spendTx.addSignature(people.get(1).sign(spendTx.getRawDataToSign(0)), 0);
        spendTx.finalize();
        newBlock.addTransaction(spendTx);
        newBlock.finalize();

        assertTrue("Null created block", createdBlock != null);
        assertTrue("Created block has wrong parent",
                createdBlock.getPrevBlockHash().equals(genesisBlock.getHash()));
        assertTrue("Created block has "+createdBlock.getTransactions().size()+
                " tranactions", createdBlock.getTransactions().size() == 1);
        assertTrue("Wrong transaction",
                createdBlock.getTransaction(0).equals(spendCoinbaseTx));
        assertTrue("Failed to process block",blockHandler.processBlock(newBlock));
    }


    @Test
    //Process a transaction, create a block,
    //then process a block on top of the genesis block
    //with a transaction claiming a UTXO from that transaction
    public void test25() {
        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Transaction spendCoinbaseTx = new Transaction();
        spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
        spendCoinbaseTx.finalize();
        blockHandler.processTx(spendCoinbaseTx);

        Block createdBlock = blockHandler.createBlock(people.get(1).getPublicKey());

        Block newBlock = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
        Transaction spendTx = new Transaction();
        spendTx.addInput(spendCoinbaseTx.getHash(), 0);
        spendTx.addOutput(Block.COINBASE, people.get(2).getPublicKey());
        spendTx.addSignature(people.get(1).sign(spendTx.getRawDataToSign(0)), 0);
        spendTx.finalize();
        newBlock.addTransaction(spendTx);
        newBlock.finalize();

        assertTrue("Null created block", createdBlock != null);
        assertTrue("Created block has wrong parent",
                createdBlock.getPrevBlockHash().equals(genesisBlock.getHash()));
        assertTrue("Created block has "+createdBlock.getTransactions().size()+
                " tranactions", createdBlock.getTransactions().size() == 1);
        assertTrue("Wrong transaction",
                createdBlock.getTransaction(0).equals(spendCoinbaseTx));
        assertFalse("Process block should fail",blockHandler.processBlock(newBlock));
    }

    @Test
    //Process multiple blocks directly on top of the genesis block,
    //then create a block. Oldest block at the same height as
    //maxHeightBlock should be maxHeightBlock.
    public void test18() {
        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block block;
        Block firstBlock = null;
        Transaction spendCoinbaseTx;

        for (int i = 0; i < 100; i++) {
            block = new Block(genesisBlock.getHash(), people.get(1).getPublicKey());
            if (i == 0)
                firstBlock = block;

            spendCoinbaseTx = new Transaction();
            spendCoinbaseTx.addInput(genesisBlock.getCoinbase().getHash(), 0);
            spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
            spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
            spendCoinbaseTx.finalize();
            block.addTransaction(spendCoinbaseTx);
            block.finalize();
            assertTrue(String.format("(%d) Failed to process block", i),
                    blockHandler.processBlock(block));
            assertTrue("MaxHeightBlock changed @ pass "+i,
                    blockChain.getMaxHeightBlock()==firstBlock);
        }

        Block createdBlock = blockHandler.createBlock(people.get(1).getPublicKey());

        assertTrue("Null created block", createdBlock != null);
        assertTrue("Created block has wrong parent",
                createdBlock.getPrevBlockHash().equals(firstBlock.getHash()));
        assertTrue("Created block has "+createdBlock.getTransactions().size()+
                " tranactions", createdBlock.getTransactions().size() == 0);
    }



    @Test
    //Construct two branches of approximately equal size,
    //ensuring that blocks are always created on the proper branch
    public void test26() {

        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        boolean flipped = false;
        Block block;
        Block firstBranchPrevBlock = genesisBlock;
        Block secondBranchPrevBlock = genesisBlock;
        Transaction spendCoinbaseTx;

        for (int i = 0; i < 30; i++) {
            spendCoinbaseTx = new Transaction();
            String item = String.format("(%d) ", i);
            if (i % 2 == 0) {
                if (!flipped) {
                    spendCoinbaseTx.addInput(firstBranchPrevBlock.getCoinbase().getHash(), 0);
                    spendCoinbaseTx.addOutput(Block.COINBASE, people.get(0).getPublicKey());
                    spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                    spendCoinbaseTx.finalize();
                    blockHandler.processTx(spendCoinbaseTx);

                    block = blockHandler.createBlock(people.get(0).getPublicKey());
                    assertTrue(item+"Null block",
                            block != null);
                    assertTrue(item+"Block on wrong branch",
                            block.getPrevBlockHash().equals(firstBranchPrevBlock.getHash()) );
                    assertTrue(item+"Block size = "+block.getTransactions().size(),
                            block.getTransactions().size() == 1);
                    assertTrue(item+"Wrong transaction",
                            block.getTransaction(0).equals(spendCoinbaseTx));
                    firstBranchPrevBlock = block;
                } else {
                    spendCoinbaseTx.addInput(secondBranchPrevBlock.getCoinbase().getHash(), 0);
                    spendCoinbaseTx.addOutput(Block.COINBASE, people.get(0).getPublicKey());
                    spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                    spendCoinbaseTx.finalize();
                    blockHandler.processTx(spendCoinbaseTx);

                    block = blockHandler.createBlock(people.get(0).getPublicKey());

                    assertTrue(item+"Null block",
                            block != null);
                    assertTrue(item+"Block on wrong branch",
                            block.getPrevBlockHash().equals(secondBranchPrevBlock.getHash()) );
                    assertTrue(item+"Block size = "+block.getTransactions().size(),
                            block.getTransactions().size() == 1);
                    assertTrue(item+"Wrong transaction",
                            block.getTransaction(0).equals(spendCoinbaseTx));

                    secondBranchPrevBlock = block;
                }
            } else {
                if (!flipped) {
                    // add two blocks two second branch
                    block = new Block(secondBranchPrevBlock.getHash(), people.get(0).getPublicKey());
                    spendCoinbaseTx = new Transaction();
                    spendCoinbaseTx.addInput(secondBranchPrevBlock.getCoinbase().getHash(), 0);
                    spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
                    spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                    spendCoinbaseTx.finalize();
                    block.addTransaction(spendCoinbaseTx);
                    block.finalize();

                    assertTrue(item+"Failed to process block",
                            blockHandler.processBlock(block));
                    secondBranchPrevBlock = block;

                    block = new Block(secondBranchPrevBlock.getHash(), people.get(0).getPublicKey());
                    spendCoinbaseTx = new Transaction();
                    spendCoinbaseTx.addInput(secondBranchPrevBlock.getCoinbase().getHash(), 0);
                    spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
                    spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                    spendCoinbaseTx.finalize();
                    block.addTransaction(spendCoinbaseTx);
                    block.finalize();

                    assertTrue(item+"Failed to process block",
                            blockHandler.processBlock(block));
                    secondBranchPrevBlock = block;

                    if (i > 1) {
                        block = new Block(secondBranchPrevBlock.getHash(), people.get(0).getPublicKey());
                        spendCoinbaseTx = new Transaction();
                        spendCoinbaseTx.addInput(secondBranchPrevBlock.getCoinbase().getHash(), 0);
                        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
                        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                        spendCoinbaseTx.finalize();
                        block.addTransaction(spendCoinbaseTx);
                        block.finalize();

                        assertTrue(item+"Failed to process block",
                                blockHandler.processBlock(block));
                        secondBranchPrevBlock = block;
                    }
                } else {
                    block = new Block(firstBranchPrevBlock.getHash(), people.get(0).getPublicKey());
                    spendCoinbaseTx = new Transaction();
                    spendCoinbaseTx.addInput(firstBranchPrevBlock.getCoinbase().getHash(), 0);
                    spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
                    spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                    spendCoinbaseTx.finalize();
                    block.addTransaction(spendCoinbaseTx);
                    block.finalize();

                    assertTrue(item+"Failed to process block",
                            blockHandler.processBlock(block));
                    firstBranchPrevBlock = block;

                    block = new Block(firstBranchPrevBlock.getHash(), people.get(0).getPublicKey());
                    spendCoinbaseTx = new Transaction();
                    spendCoinbaseTx.addInput(firstBranchPrevBlock.getCoinbase().getHash(), 0);
                    spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
                    spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                    spendCoinbaseTx.finalize();
                    block.addTransaction(spendCoinbaseTx);
                    block.finalize();

                    assertTrue(item+"Failed to process block",
                            blockHandler.processBlock(block));
                    firstBranchPrevBlock = block;

                    if (i > 1) {
                        block = new Block(firstBranchPrevBlock.getHash(), people.get(0).getPublicKey());
                        spendCoinbaseTx = new Transaction();
                        spendCoinbaseTx.addInput(firstBranchPrevBlock.getCoinbase().getHash(), 0);
                        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
                        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                        spendCoinbaseTx.finalize();
                        block.addTransaction(spendCoinbaseTx);
                        block.finalize();

                        assertTrue(item+"Failed to process block",
                                blockHandler.processBlock(block));
                        firstBranchPrevBlock = block;
                    }
                }
                flipped = !flipped;
            }
        }
    }

    private class ForwardBlockNode {
        public Block b;
        public ForwardBlockNode child;

        public ForwardBlockNode(Block b) {
            this.b = b;
            this.child = null;
        }

        public void setChild(ForwardBlockNode child) {
            this.child = child;
        }
    }



    @Test
    //Similar to previous test, but then try to process blocks
    //whose parents are at height < maxHeight - CUT_OFF_AGE"
    public void test27() {
        Block genesisBlock = new Block(null, people.get(0).getPublicKey());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        boolean flipped = false;
        Block block;
        Block firstBranchPrevBlock = genesisBlock;
        ForwardBlockNode firstBranch = new ForwardBlockNode(firstBranchPrevBlock);
        ForwardBlockNode firstBranchTracker = firstBranch;
        Block secondBranchPrevBlock = genesisBlock;
        ForwardBlockNode secondBranch = new ForwardBlockNode(secondBranchPrevBlock);
        ForwardBlockNode secondBranchTracker = secondBranch;
        Transaction spendCoinbaseTx;

        for (int i = 0; i < 3*BlockChain.CUT_OFF_AGE; i++) {
            spendCoinbaseTx = new Transaction();
            String item = String.format("(%d) ", 1);
            if (i % 2 == 0) {
                if (!flipped) {
                    spendCoinbaseTx.addInput(firstBranchPrevBlock.getCoinbase().getHash(), 0);
                    spendCoinbaseTx.addOutput(Block.COINBASE, people.get(0).getPublicKey());
                    spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                    spendCoinbaseTx.finalize();
                    blockHandler.processTx(spendCoinbaseTx);

                    block = blockHandler.createBlock(people.get(0).getPublicKey());

                    assertTrue(item+"Null block", block != null);
                    assertTrue(item+"Wrong parent",
                            block.getPrevBlockHash().equals(firstBranchPrevBlock.getHash()) );
                    assertTrue(item+"# of transactions = "+block.getTransactions().size(),
                            block.getTransactions().size() == 1);
                    assertTrue(item+"Transaction not the coinbase",
                            block.getTransaction(0).equals(spendCoinbaseTx));

                    ForwardBlockNode newNode = new ForwardBlockNode(block);
                    firstBranchTracker.setChild(newNode);
                    firstBranchTracker = newNode;
                    firstBranchPrevBlock = block;
                } else {
                    spendCoinbaseTx.addInput(secondBranchPrevBlock.getCoinbase().getHash(), 0);
                    spendCoinbaseTx.addOutput(Block.COINBASE, people.get(0).getPublicKey());
                    spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                    spendCoinbaseTx.finalize();
                    blockHandler.processTx(spendCoinbaseTx);

                    block = blockHandler.createBlock(people.get(0).getPublicKey());

                    assertTrue(item+"Null block", block != null);
                    assertTrue(item+"Wrong parent",
                            block.getPrevBlockHash().equals(secondBranchPrevBlock.getHash()) );
                    assertTrue(item+"# of transactions = "+block.getTransactions().size(),
                            block.getTransactions().size() == 1);
                    assertTrue(item+"Transaction not the coinbase",
                            block.getTransaction(0).equals(spendCoinbaseTx));

                    ForwardBlockNode newNode = new ForwardBlockNode(block);
                    secondBranchTracker.setChild(newNode);
                    secondBranchTracker = newNode;
                    secondBranchPrevBlock = block;
                }
            } else {
                if (!flipped) {
                    // add two blocks two second branch
                    block = new Block(secondBranchPrevBlock.getHash(), people.get(0).getPublicKey());
                    spendCoinbaseTx = new Transaction();
                    spendCoinbaseTx.addInput(secondBranchPrevBlock.getCoinbase().getHash(), 0);
                    spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
                    spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                    spendCoinbaseTx.finalize();
                    block.addTransaction(spendCoinbaseTx);
                    block.finalize();

                    assertTrue(item+"Block not added",
                            blockHandler.processBlock(block));
                    ForwardBlockNode newNode = new ForwardBlockNode(block);
                    secondBranchTracker.setChild(newNode);
                    secondBranchTracker = newNode;
                    secondBranchPrevBlock = block;

                    block = new Block(secondBranchPrevBlock.getHash(), people.get(0).getPublicKey());
                    spendCoinbaseTx = new Transaction();
                    spendCoinbaseTx.addInput(secondBranchPrevBlock.getCoinbase().getHash(), 0);
                    spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
                    spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                    spendCoinbaseTx.finalize();
                    block.addTransaction(spendCoinbaseTx);
                    block.finalize();

                    assertTrue(item+"Block not added",
                            blockHandler.processBlock(block));
                    newNode = new ForwardBlockNode(block);
                    secondBranchTracker.setChild(newNode);
                    secondBranchTracker = newNode;
                    secondBranchPrevBlock = block;

                    if (i > 1) {
                        block = new Block(secondBranchPrevBlock.getHash(), people.get(0).getPublicKey());
                        spendCoinbaseTx = new Transaction();
                        spendCoinbaseTx.addInput(secondBranchPrevBlock.getCoinbase().getHash(), 0);
                        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
                        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                        spendCoinbaseTx.finalize();
                        block.addTransaction(spendCoinbaseTx);
                        block.finalize();

                        assertTrue(item+"Block not added",
                                blockHandler.processBlock(block));

                        newNode = new ForwardBlockNode(block);
                        secondBranchTracker.setChild(newNode);
                        secondBranchTracker = newNode;
                        secondBranchPrevBlock = block;
                    }
                } else {
                    block = new Block(firstBranchPrevBlock.getHash(), people.get(0).getPublicKey());
                    spendCoinbaseTx = new Transaction();
                    spendCoinbaseTx.addInput(firstBranchPrevBlock.getCoinbase().getHash(), 0);
                    spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
                    spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                    spendCoinbaseTx.finalize();
                    block.addTransaction(spendCoinbaseTx);
                    block.finalize();

                    assertTrue(item+"Block not added",
                            blockHandler.processBlock(block));

                    ForwardBlockNode newNode = new ForwardBlockNode(block);
                    firstBranchTracker.setChild(newNode);
                    firstBranchTracker = newNode;
                    firstBranchPrevBlock = block;

                    block = new Block(firstBranchPrevBlock.getHash(), people.get(0).getPublicKey());
                    spendCoinbaseTx = new Transaction();
                    spendCoinbaseTx.addInput(firstBranchPrevBlock.getCoinbase().getHash(), 0);
                    spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
                    spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                    spendCoinbaseTx.finalize();
                    block.addTransaction(spendCoinbaseTx);
                    block.finalize();

                    assertTrue(item+"Block not added",
                            blockHandler.processBlock(block));

                    newNode = new ForwardBlockNode(block);
                    firstBranchTracker.setChild(newNode);
                    firstBranchTracker = newNode;
                    firstBranchPrevBlock = block;

                    if (i > 1) {
                        block = new Block(firstBranchPrevBlock.getHash(), people.get(0).getPublicKey());
                        spendCoinbaseTx = new Transaction();
                        spendCoinbaseTx.addInput(firstBranchPrevBlock.getCoinbase().getHash(), 0);
                        spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
                        spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
                        spendCoinbaseTx.finalize();
                        block.addTransaction(spendCoinbaseTx);
                        block.finalize();

                        assertTrue(item+"Block not added",
                                blockHandler.processBlock(block));

                        newNode = new ForwardBlockNode(block);
                        firstBranchTracker.setChild(newNode);
                        firstBranchTracker = newNode;
                        firstBranchPrevBlock = block;
                    }
                }
                flipped = !flipped;
            }
        }



        int firstBranchHeight = 0;
        firstBranchTracker = firstBranch;
        while (firstBranchTracker != null) {
            firstBranchTracker = firstBranchTracker.child;
            firstBranchHeight++;
        }

        int secondBranchHeight = 0;
        secondBranchTracker = secondBranch;
        while (secondBranchTracker != null) {
            secondBranchTracker = secondBranchTracker.child;
            secondBranchHeight++;
        }

        int maxHeight = Math.max(firstBranchHeight, secondBranchHeight);

        int firstBranchCount = 0;
        firstBranchTracker = firstBranch;
        while (firstBranchTracker.child != null) {
            block = new Block(firstBranchTracker.b.getHash(), people.get(0).getPublicKey());
            spendCoinbaseTx = new Transaction();
            spendCoinbaseTx.addInput(firstBranchTracker.b.getCoinbase().getHash(), 0);
            spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
            spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
            spendCoinbaseTx.finalize();
            block.addTransaction(spendCoinbaseTx);
            block.finalize();

            if (firstBranchCount < maxHeight - BlockChain.CUT_OFF_AGE - 1) {
                assertFalse("Block should not be added",
                        blockHandler.processBlock(block));
            } else {
                assertTrue("Block not added",
                        blockHandler.processBlock(block));
            }

            firstBranchTracker = firstBranchTracker.child;
            firstBranchCount++;
        }

        int secondBranchCount = 0;
        secondBranchTracker = secondBranch;
        while (secondBranchTracker != null) {
            block = new Block(secondBranchTracker.b.getHash(), people.get(0).getPublicKey());
            spendCoinbaseTx = new Transaction();
            spendCoinbaseTx.addInput(secondBranchTracker.b.getCoinbase().getHash(), 0);
            spendCoinbaseTx.addOutput(Block.COINBASE, people.get(1).getPublicKey());
            spendCoinbaseTx.addSignature(people.get(0).sign(spendCoinbaseTx.getRawDataToSign(0)), 0);
            spendCoinbaseTx.finalize();
            block.addTransaction(spendCoinbaseTx);
            block.finalize();

            if (secondBranchCount < maxHeight - BlockChain.CUT_OFF_AGE - 1) {
                assertFalse("Block should not be added",
                        blockHandler.processBlock(block));
            } else {
                assertTrue("Block not added",
                        blockHandler.processBlock(block));

            }

            secondBranchTracker = secondBranchTracker.child;
            secondBranchCount++;
        }
    }


    }
