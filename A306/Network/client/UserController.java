package Network.client;

import RSA.*;
import robin.Block;
import robin.DatabaseConnection;
import robin.Message;

import java.io.*;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class UserController
{
    private RSAKeyPair userKeyPair;
    private HashMap<Integer, String> allKnownNodes;
    private DatabaseConnection sqlConnection;
    private long currentIndex;

    public UserController()
    {
        readKeysAndNodes();
    }

    private void readKeysAndNodes()
    {
        File knownNodes = new File(new File("").getAbsolutePath()+"/SavedNodes.txt");
        File userKeys = new File(new File("").getAbsolutePath()+"/MyKeys.txt");

        try {
            allKnownNodes = readAllNodes(knownNodes);
            sqlConnection = new DatabaseConnection(
                    new File(new File("").getAbsolutePath()+"/p2_localclient_blockchain.db").toString());
            currentIndex = sqlConnection.getBlockCount();

            if (userKeys.length() == 0)
                writeKeys(userKeys);
            else
                readKeys(userKeys);
        }
        catch (IOException | SQLException e) { e.printStackTrace(); }
    }

    public void sendMessage(String msg, RSAKey recipient)
    {
        RSAOAEPEncrypt encryptedmsg;
        RSAOAEPSign signature;
        Message strToSend;

        try {
            encryptedmsg = new RSAOAEPEncrypt(msg, recipient);
            signature = new RSAOAEPSign(recipient.getBase64String() + userKeyPair.getPublicKey().getBase64String() +
                        encryptedmsg.getEncryptedMessageBase64String(), userKeyPair.getPrivateKey());

            strToSend = new Message(encryptedmsg.getEncryptedMessageBase64String(),
                    userKeyPair.getPublicKey(), recipient, new Signature(signature.getSignatureBase64String()));

            UserConnection toConnect;
            for (Integer a : allKnownNodes.keySet())
            {
                try {
                    toConnect = new UserConnection(allKnownNodes.get(a), a);
                    toConnect.start();
                    toConnect.sendMsg(strToSend);
                } catch (IOException e) { continue; }
            }

        }
        catch (IOException e) { e.printStackTrace(); }
    }
    public void sendMessage(String msg)
    {
        RSAOAEPEncrypt encryptedmsg;
        RSAOAEPSign signature;
        Message strToSend;

        try {
            encryptedmsg = new RSAOAEPEncrypt(msg, getUserPublicKey());
            signature = new RSAOAEPSign(getUserPublicKey().getBase64String() + userKeyPair.getPublicKey().getBase64String() +
                    encryptedmsg.getEncryptedMessageBase64String(), userKeyPair.getPrivateKey());

            strToSend = new Message(encryptedmsg.getEncryptedMessageBase64String(),
                    userKeyPair.getPublicKey(), getUserPublicKey(), new Signature(signature.getSignatureBase64String()));

            UserConnection toConnect;
            for (Integer a : allKnownNodes.keySet())
            {
                try {
                    toConnect = new UserConnection(allKnownNodes.get(a), a);
                    toConnect.start();
                    toConnect.sendMsg(strToSend);
                } catch (IOException e) { continue; }
            }

        }
        catch (IOException e) { e.printStackTrace(); }
    }

    private void readKeys(File file) throws IOException
    {
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(file));
        int keysRead = 0;
        RSAKey userPublicKey = null, userPrivateKey = null;

        while ((line = reader.readLine()) != null)
        {
            try {
                if (keysRead == 0) {
                    userPublicKey = new RSAKey(line);
                    keysRead++;
                }
                else if (keysRead == 1) {
                    userPrivateKey = new RSAKey(line);
                    userKeyPair = new RSAKeyPair(userPrivateKey, userPublicKey);
                }

            }
            catch (InvalidRSAKeyException e) {e.printStackTrace();}
        }
        reader.close();
    }
    private void writeKeys (File file) throws IOException
    {
        KeyPairGenerator generator = new KeyPairGenerator(2048);
        userKeyPair = generator.generateKeyPair();

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        writer.write(userKeyPair.getPublicKey().getBase64String());
        writer.write("\n");
        writer.write(userKeyPair.getPrivateKey().getBase64String());
        writer.close();
    }
    public void getLatestBlockIndex () throws SQLException
    {
        UserConnection toConnect;
        UserConnection toRequest;
        long blockIndex;
        ArrayList<Block> blocksToAdd;
        boolean toAddBlock = false;

        for (Integer a : allKnownNodes.keySet()) {
            try {
                toConnect = new UserConnection(allKnownNodes.get(a), a);
                toConnect.start();
                blockIndex = toConnect.requestBlockIndex();

                if (blockIndex > currentIndex) {
                    System.out.println("Remote blockindex "+blockIndex+", current index "+currentIndex);
                    toRequest = new UserConnection(allKnownNodes.get(a), a);
                    toRequest.start();
                    blocksToAdd = toRequest.requestBlocksByIndex(currentIndex);

                    for(Block b : blocksToAdd) {
                        toAddBlock = false;
                        try {
                            for (Message m : b.getMessages()) {
                                if (m.getRecipientPublicKey().equals(getUserPublicKey()))
                                    toAddBlock = true;
                            }
                        }
                        catch (NullPointerException e) { continue; }
                        if (toAddBlock) {
                            sqlConnection.addBlock(b);
                        }
                    }
                    System.out.println("Blocks "+currentIndex+" - "+blockIndex+" retrieved to local database");
                    currentIndex = blockIndex;
                }
            }
            catch (IOException e) { e.printStackTrace();}
        }
    }
    public RSAKey getUserPrivateKey() { return userKeyPair.getPublicKey(); }
    public RSAKey getUserPublicKey() { return userKeyPair.getPrivateKey(); }
    private HashMap<Integer, String> readAllNodes(File fileToRead) throws IOException
    {
        HashMap<Integer, String> allFoundNodes = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(fileToRead));
        String line;

        while ((line = reader.readLine()) != null)
        {
            String[] parts = line.split(":", 2);
            if (parts.length >= 2)
            {
                Integer key = Integer.valueOf(parts[1]);
                String value = parts[0];
                allFoundNodes.put(key, value);
            } else {
                System.out.println("ignoring line: " + line);
            }
        }
        reader.close();

        return allFoundNodes;
    }
}
