package Network.node;

import Network.GSON.*;
import RSA.BadVerificationException;
import RSA.RSAOAEPVerify;
import robin.*;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NodeThread implements Runnable
{
    private BufferedReader is;
    private PrintStream os;
    private Socket clientSocket;
    private ConcurrentLinkedQueue<Message> emailToMine;
    private DatabaseConnection sqlConnection;
    private NodeClient client;
    // A block must at max be 2 hours ahead.
    private final int maxBlockTimeAhead = 2 * 60 * 60 * 1000;

    public NodeThread(Socket clientSocket, ConcurrentLinkedQueue<Message> emailsToMine, DatabaseConnection sqlConnection, NodeClient client) {
        this.sqlConnection = sqlConnection;
        this.clientSocket = clientSocket;
        this.emailToMine = emailsToMine;
        this.client = client;
    }

    @Override
    public void run()
    {
        String message;
        JSONClass JSONConverter;
        NetworkCommand command;

        try {
            is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            os = new PrintStream(clientSocket.getOutputStream());

            while (true) {
                message = is.readLine();

                if (!message.isEmpty()) {
                    JSONConverter = new JSONClass(message);
                    command = JSONConverter.getCommand();

                    if (command == null)
                        break;

                    switch (command.getCommand()) {
                        case SEND_MESSAGE:
                            includeMessage(JSONConverter);
                            break;
                        case GET_BLOCK_COUNT:
                            os.println(sqlConnection.getBlockCount());
                            break;
                        case GET_BLOCK_BY_INDEX:
                            sendBlocksByIndex(JSONConverter);
                            break;
                        case BLOCK_MINED:
                            includeBlock(JSONConverter);
                            break;
                        default:
                            // Unsupported command.
                            break;
                    }
                    break;
                }
            }
            is.close();
            os.close();
        }
        catch (IOException | SQLException e) { e.printStackTrace(); }
    }
    private void includeMessage(JSONClass converter)
    {
        JSONMessage messageCommand = converter.getJSONMessage();
        Message incomingMessage = messageCommand.getMessage();

        if (isMessageSignatureValid(incomingMessage))
            emailToMine.add(incomingMessage);
    }
    private void includeBlock(JSONClass converter) throws SQLException
    {
        JSONBlock blockCommand = converter.getJSONBlock();
        Block incomingBlock = blockCommand.getBlock();

        if (isBlockValid(incomingBlock, sqlConnection.getLatestBlock())) {
            client.stopMining();
            emailToMine.removeAll(incomingBlock.getMessages());
            sqlConnection.addBlock(incomingBlock);
            client.beginMining();
            System.out.println("Remote block was valid");
        }
        else
            System.out.println("Block was not valid");
    }
    private void sendBlocksByIndex(JSONClass converter)
    {
        JSONBlocksByIndex byIndex = converter.getJSONBlocksByIndex();

        try {
            long blocksToSend = sqlConnection.getBlockCount() - byIndex.getIndex();
            for (long i = sqlConnection.getBlockCount() - blocksToSend; i <= sqlConnection.getBlockCount(); i++ ) {
                converter = new JSONClass(sqlConnection.getBlockByIndex(i));
                os.println(converter.makeJSONBlockString());
            }
            os.println(".done");
        }
        catch (SQLException e) { e.printStackTrace(); }
    }
    // Checks if incomming message from client has valid signature.
    private boolean isMessageSignatureValid(Message message) {
        try {
            new RSAOAEPVerify(message);
            return true;
        }
        catch (IOException | BadVerificationException e) {
            return false;
        }
    }
    // Checks if incomming block is valid.
    private boolean isBlockValid(Block newBlock, Block previousBlock) {
        long blockCount = previousBlock.getIndex() + 1;
        boolean isMessagesValid = true;

        for (Message message : newBlock.getMessages()) {
            if (!isMessageSignatureValid(message)) {
                isMessagesValid = false;
                System.out.println("Message invalid");
            }
        }
        /* Checks if the new block is correct. Checks if the following is correct:
         * Previous hash.
         * The block contains messages.
         * Nounce not created with a timestamp futher ahead than 2 hours.
         * Proof-of-work target is valid.
         * Messages in block are all of valid signatures.
         * Merkle root.
         */
        return isPreviousHashCorrect(previousBlock, newBlock.getPrevHeadHash()) &
                !(newBlock.getMessages() == null || newBlock.getMessages().size() < 1) &
                !(newBlock.getTimestamp() - new Date().getTime() > maxBlockTimeAhead) &
                isProofOfWorkValid(newBlock) &
                isTargetValid(newBlock, previousBlock, blockCount) &
                isMessagesValid &
                isMerkleRootHashValid(newBlock);
    }
    private boolean isPreviousHashCorrect(Block previousBlock, String previousHeadHash)
    {
        if (!previousBlock.getHash().equals(previousHeadHash))
            System.out.println("PrevHash incorrect");

        return previousBlock.getHash().equals(previousHeadHash);
    }
    // Checks if proof-of-work target is valid.
    private boolean isProofOfWorkValid(Block newBlock)
    {
        BigInteger blockHash = new BigInteger(newBlock.calculateHash(), 16);
        BigInteger target = new Target(newBlock.getCompactTarget()).getBigIntegerTarget();

        if (!(blockHash.compareTo(target) <= 0))
            System.out.println("Proof-of-work incorrect");

        return blockHash.compareTo(target) <= 0;
    }
    // Checks if Merkle root is correct.
    private boolean isMerkleRootHashValid(Block newBlock)
    {
        String realMerkleRootHash = BlockUtil.calculateMerkleRootHash(newBlock.getMessages());

        if (!(realMerkleRootHash.equals(newBlock.getMerkleRootHash())))
            System.out.println("Merkle incorrect");

        return realMerkleRootHash.equals(newBlock.getMerkleRootHash());
    }
    // Checks if the target of incomming block is correct.
    private boolean isTargetValid(Block newBlock, Block previousBlock, long newBlockIndex) {
        String newBlockCompactTarget = newBlock.getCompactTarget();

        // If the blockchain.block before incomming block is a multiple of the target adjust interval, first adjust local blockchain target.
        if ((newBlockIndex - 1) % TargetUtil.getTargetAdjustInterval() == 0 && (newBlockIndex - 1) > 0) {
            try {
                // Start period blockchain.block == previous blockchain.block - blockchain.target adjust interval.
                Block startPeriodBlock = sqlConnection.getBlockByIndex(newBlockIndex - 1 - TargetUtil.getTargetAdjustInterval());

                if (!(newBlockCompactTarget.equals(TargetUtil.adjustTarget(previousBlock, startPeriodBlock).getCompactTarget())))
                    System.out.println("Target invalid");

                return newBlockCompactTarget.equals(TargetUtil.adjustTarget(previousBlock, startPeriodBlock).getCompactTarget());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        // Otherwise the blockchain.target should be the same as in the previous blockchain.block.
        else {
            if (!(newBlockCompactTarget.equals(previousBlock.getCompactTarget())))
                System.out.println("Target invalid");

            return newBlockCompactTarget.equals(previousBlock.getCompactTarget());
        }
    }
}
