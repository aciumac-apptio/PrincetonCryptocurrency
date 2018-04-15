// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.*;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    private boolean hasChanged;

    private TransactionPool transactionPool;
    private BlockNode genesisNode;

    private class BlockNode {
        public Block block;
        public BlockNode parent;
        public int height;
        public UTXOPool utxoPool;
        public List<BlockNode> children;

        public BlockNode(Block block, BlockNode parent, int height, List<BlockNode> children, UTXOPool utxoPool) {
            this.block = block;
            this.parent = parent;
            this.height = height;
            this.utxoPool = utxoPool;
            this.children = children;
        }

        public BlockNode(Block block) {
            this(block, null, 0, new ArrayList<BlockNode>(), new UTXOPool());
        }
    }
    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS

        //this.genesisBlock = genesisBlock;
        this.genesisNode = new BlockNode(genesisBlock);


        // Add coinbase to this utxo pool
        byte[] txHash = new byte[]{};
        UTXO utxo = new UTXO(txHash, 0);
        Transaction.Output output = genesisBlock.getCoinbase().getOutput(0);

        this.genesisNode.utxoPool.addUTXO(utxo, output);
        this.transactionPool = new TransactionPool();

        //Add coinbase to transaction pool
        transactionPool.addTransaction(genesisBlock.getCoinbase());

        hasChanged = false;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return getMaxHeightBlockNode(genesisNode).block;
    }

    /** Get the src.main.UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return getMaxHeightBlockNode(genesisNode).utxoPool;
    }

    private BlockNode getMaxHeightBlockNode(BlockNode node) {
        if (node.children.isEmpty()) {
            return node;
        } else {
            int height = node.height;
            List<BlockNode> list = node.children;
            for (BlockNode nd : list) {
/*                if (getMaxHeightBlockNode(nd).height > height) {
                    node = nd;
                    height = nd.height;
                }*/
                nd = getMaxHeightBlockNode(nd);
                if (nd.height > height) {
                    node = nd;
                    height = nd.height;
                }
            }
        }
        return node;
    }




    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return transactionPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS

        if (genesisNode != null && block.getPrevBlockHash() == null ) {
            return false;
        }

/*        Transaction blockCoinbase = block.getCoinbase();
        Transaction tx = new Transaction(blockCoinbase.getOutput(0).value, blockCoinbase.getOutput(0).address)
        tx.s
        if ()*/

        genesisNode = addBlock(genesisNode, block);

        if (hasChanged) {
            List<Transaction> transactions = block.getTransactions();
            for (Transaction transaction: transactions) {
                transactionPool.removeTransaction(transaction.getHash());
            }
            transactionPool.addTransaction(block.getCoinbase());

            hasChanged = false;
            return true;
        } else {
            return false;
        }
    }


    private BlockNode addBlock(BlockNode node, Block b) {
        //node.children.isEmpty() ||
        if (new ByteArrayWrapper(node.block.getHash()).equals(new ByteArrayWrapper(b.getPrevBlockHash()))) {

            int childHeight = node.height + 1;
            int cutOffHeight = getMaxHeightBlockNode(genesisNode).height - CUT_OFF_AGE;

            if (childHeight <= cutOffHeight) {
                return node;
            }

            //add coinbase transaction to copyofnodepool
            // before processing

            UTXOPool copyOfNodePool = new UTXOPool(node.utxoPool);
            TxHandler txHandler = new TxHandler(copyOfNodePool);
            Transaction[] txArray = b.getTransactions().toArray(new Transaction[b.getTransactions().size()]);
            Transaction[] validTxs = txHandler.handleTxs(txArray);

            // Coinbase raw data is corrupted test 6


            copyOfNodePool.addUTXO(new UTXO(b.getCoinbase().getHash(), 0), b.getCoinbase().getOutput(0));
            List<BlockNode> list = new ArrayList<>();


            node.children.add(new BlockNode(b, node, node.height + 1, list, copyOfNodePool));

            hasChanged = true;
        } else {
            /*ByteArrayWrapper parentWrapper = new ByteArrayWrapper(node.block.getHash());
            ByteArrayWrapper blockWrapper = new ByteArrayWrapper(block.getPrevBlockHash());*/

            ListIterator<BlockNode> listIterator = node.children.listIterator();
            while (listIterator.hasNext() && !hasChanged) {
                BlockNode nd = listIterator.next();
                listIterator.set(addBlock(nd, b));
            }
        }

        return node;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        transactionPool.addTransaction(tx);
    }
}