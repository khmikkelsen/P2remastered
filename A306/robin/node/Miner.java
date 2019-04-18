package robin.node;

import Network.node.NodeConnection;
import robin.Block;
import robin.TargetUtil;
import robin.DatabaseConnection;
import robin.Message;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Miner implements Runnable {

    private ConcurrentLinkedQueue<Message> incMessages;
    private DatabaseConnection databaseConnection;
    private AtomicBoolean minerToNotify;
    private ConcurrentLinkedQueue<Message> emailsToMine;
    private HashMap<Integer, String> allKnownNodes;

    public Miner(ConcurrentLinkedQueue<Message> messages, DatabaseConnection databaseConnection, AtomicBoolean lock,
                 ConcurrentLinkedQueue<Message> emailsToMine, HashMap<Integer, String> allKnownNodes) {
        this.incMessages = messages;
        this.databaseConnection = databaseConnection;
        this.minerToNotify = lock;
        this.emailsToMine = emailsToMine;
        this.allKnownNodes = allKnownNodes;
    }

    @Override
    public void run()
    {
        try {
            Block latestBlock = databaseConnection.getLatestBlock();
            Message[] messarr = incMessages.toArray(new Message[0]);
            ArrayList<Message> messages = new ArrayList<>(Arrays.asList(incMessages.toArray(messarr)));

            long newBlockIndex = latestBlock.getIndex() + 1;
            String compactTarget = latestBlock.getCompactTarget();

            // If the block before this block is a multiple of the target adjust interval, first adjust local target.
            if ((newBlockIndex - 1) % TargetUtil.getTargetAdjustInterval() == 0 && (newBlockIndex - 1) > 0) {
                System.out.println(newBlockIndex);
                // Start period block == previous block - target adjust interval.
                Block startPeriodBlock = databaseConnection.getBlockByIndex(newBlockIndex - 1 - TargetUtil.getTargetAdjustInterval());

                compactTarget = TargetUtil.adjustTarget(latestBlock, startPeriodBlock).getCompactTarget();
            }

            // Otherwise the target should be the same as in the previous block.
            Block newBlock = new Block(latestBlock.getHash(), compactTarget, messages);

            // Mine the block. This method blocks until it is done.
            newBlock.mineBlock();

            // Add block
            databaseConnection.addBlock(newBlock);
            System.out.println("I won 10 ICC (Imaginary Crypto Currency)");
            broadcastBlock(newBlock);
            emailsToMine.removeAll(messages);
            incMessages.clear();
            minerToNotify.set(true);
        }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    private void broadcastBlock (Block newBlock)
    {
        NodeConnection toConnect;
        for (Integer a : allKnownNodes.keySet()) {
            try {
                toConnect = new NodeConnection(allKnownNodes.get(a), a);
                toConnect.start();
                toConnect.sendBlock(newBlock);
                toConnect.close();
            }
            catch (IOException e) { continue; }
        }
    }
}
