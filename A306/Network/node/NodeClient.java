package Network.node;

import RSA.*;
import Network.GSON.Serializers.RSAKeyDeserializer;
import Network.GSON.Serializers.RSAKeySerializer;
import robin.*;

import com.google.gson.GsonBuilder;
import robin.node.Miner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class NodeClient
{
    private final int port;
    private volatile boolean nodeRunning;
    private volatile boolean miningRunning;
    private final ExecutorService nodeThreadPool;
    private ConcurrentLinkedQueue<Message> emailsToMine;
    private Thread incommingConnectionThread;
    private Thread miningThread;
    private Thread mineNonceThread;
    private AtomicBoolean lock;
    private static DatabaseConnection sqlConnection;
    private HashMap<Integer, String> allKnownNodes;


    public NodeClient(int port) throws IOException {
        this.port = port;
        this.nodeThreadPool = Executors.newFixedThreadPool(100, Executors.defaultThreadFactory());
        this.lock = new AtomicBoolean(true);
        this.allKnownNodes = readNodes(new File(new File("").getAbsolutePath()+"/OtherKnownNodes.txt"));
        emailsToMine = new ConcurrentLinkedQueue<>();
    }
    public NodeClient(int port, String args) throws IOException {
        this.port = port;
        this.nodeThreadPool = Executors.newFixedThreadPool(100, Executors.defaultThreadFactory());
        this.lock = new AtomicBoolean(true);
        this.allKnownNodes = readNodes(new File(new File("").getAbsolutePath()+"/OtherKnownNodes2.txt"));
        emailsToMine = new ConcurrentLinkedQueue<>();
    }

    public static void main(String[] args)
    {
        NodeClient node;

        try {
            if (args.length > 0) {
                node = new NodeClient(Integer.parseInt(args[0]));
                sqlConnection = new DatabaseConnection(
                        new File(new File("").getAbsolutePath() + "/p2_localnode_blockchain.db").toString());
            }
            else {
                node = new NodeClient(2222);
                sqlConnection = new DatabaseConnection(
                        new File(new File("").getAbsolutePath() + "/p2_localnode_blockchain.db").toString());
            }

            if (args.length == 2) {
                node = new NodeClient(Integer.parseInt(args[0]), args[1]);
                sqlConnection = new DatabaseConnection(
                        new File(new File("").getAbsolutePath() + "/p2_localnode_blockchain2.db").toString());

            }

            mineGenesisBlock();

            node.beginMining();
            node.start();
        }
        catch (SQLException | IOException e) { e.printStackTrace(); }
    }
    private void start ()
    {
        nodeRunning = true;
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(RSAKey.class, new RSAKeySerializer());
        gsonBuilder.registerTypeAdapter(RSAKey.class, new RSAKeyDeserializer());

        incommingConnectionThread = new Thread(() -> {
            try {
                ServerSocket nodeSocket = new ServerSocket(port);

                while (nodeRunning) {
                    Socket clientSocket = nodeSocket.accept();
                    nodeThreadPool.execute(new NodeThread(clientSocket, emailsToMine, sqlConnection, this));
                }
            }
            catch (IOException e) { e.printStackTrace(); }
        });
        incommingConnectionThread.setName("Connection thread");
        incommingConnectionThread.start();
    }
    public void beginMining ()
    {
        miningRunning = true;
        lock.set(true);
        miningThread = new Thread (() ->
        {
            System.out.println("Mining started");
            int sizeOfBlock = 0;
            ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();

            while (miningRunning) {
                if (!emailsToMine.isEmpty() & lock.get()) {
                    for (Message a : emailsToMine) {
                        if (!messages.contains(a) & sizeOfBlock < 1048576) {
                            messages.add(a);
                            sizeOfBlock += a.getByteSize();
                        }
                    }
                    mineNonceThread = new Thread(new Miner(messages, sqlConnection, lock, emailsToMine, allKnownNodes));
                    mineNonceThread.start();
                    sizeOfBlock = 0;
                    lock.set(false);
                }
            }
        });
        miningThread.setName("Mining thread");
        miningThread.start();
    }
    public void stop ()
    {
        nodeRunning = false;
        miningRunning = false;
        incommingConnectionThread.interrupt();
        miningThread.interrupt();
        mineNonceThread.interrupt();
        nodeThreadPool.shutdownNow();
    }
    public void stopMining()
    {
        mineNonceThread.stop();
        miningThread.stop();
    }
    private static void mineGenesisBlock() {
        try {
            if (sqlConnection.getBlockCount() != 0) {
                return;
            }

            RSAKey genesisPrivateKey = new RSAKey("MIICCQKCAQEAgu1qZL08/NV5d423pAi1IA8IxzSROVl6t4zWRstR9Olrpyb4QNRfg5kzN7XpcmmXcIDAEu32u1JAKlRNENpERH21odU99g7Rocxz6gCtVK76ucti4cn4wgIgQbzA03Jn01rGo4WDUCgPQUZpdQJQBi4+uGp9tDPQCswVpSAbWXxvQwLlpZ0IohfdjHo0axpcsDt9spPIIHceoLPLkGildS0ye+IO50wfYfsM3lZSlVSiWUIITOqJQtWcSo7ebGIDyeC1r5Th0dCpKSPjAtAACwoonoRVJZaVVFpAq0AS67TKQOHStopKmoBbsO84L+LQ4+47DveuDxcWac05z00jtQKCAQACeiCYGSl2eh54cn6zNIlNZY4WYL+k91+rlIH4UxVGSNqaeLxb06Nz+gRz98sxI4oeh4e6w/RLUnt4IYystohx8vPqWikNhvW/NdRKl3/aro3RUTsEx71fBEsdG0ewUxrfmY57s7AKkUYf8h0Bfk1KdKUHTa0k/pGffAF71IntQoxqfsPRVgHgQeQ0BN7OuZc+dDLxbA5MEGRSyWzxmc01P6VP2pmndG3WE9l4q5z84m4AunBMA/5tr7bB4I9I49X2J5D/TEDWTCaU1YWia7t2ljdl9c/K5rdbDQ1ZHPrAZc4BZWpvnlr72dNbAu2sU/AXDzGSJn4w+nZUgB03wcNJ");
            RSAKey genesisPublicKey = new RSAKey("MIIBCgKCAQEAgu1qZL08/NV5d423pAi1IA8IxzSROVl6t4zWRstR9Olrpyb4QNRfg5kzN7XpcmmXcIDAEu32u1JAKlRNENpERH21odU99g7Rocxz6gCtVK76ucti4cn4wgIgQbzA03Jn01rGo4WDUCgPQUZpdQJQBi4+uGp9tDPQCswVpSAbWXxvQwLlpZ0IohfdjHo0axpcsDt9spPIIHceoLPLkGildS0ye+IO50wfYfsM3lZSlVSiWUIITOqJQtWcSo7ebGIDyeC1r5Th0dCpKSPjAtAACwoonoRVJZaVVFpAq0AS67TKQOHStopKmoBbsO84L+LQ4+47DveuDxcWac05z00jtQIDAQAB");

            Message genesisMessage = new Message("Genesis block", genesisPublicKey, genesisPublicKey);
            genesisMessage.signMessage(genesisPrivateKey);

            Block genesisBlock = new Block("00007836244f37d24bf9b4f59fbe51ee85d54f12cc2349fde04199b5dd969013",
                    "0000000000000000000000000000000000000000000000000000000000000000",
                    "1e100000",
                    144267,
                    "62e9c20979b19211cd82d33f2ece5bc441c02714907dc3bb969f06bc1901ae7b",
                    1527525138304L,
                    0,
                    Collections.singletonList(genesisMessage));

            sqlConnection.addBlock(genesisBlock);
        } catch (SQLException | InvalidRSAKeyException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    private HashMap<Integer, String> readNodes (File file) throws IOException
    {
        HashMap<Integer, String> allFoundNodes = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
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
