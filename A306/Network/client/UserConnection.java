package Network.client;

import Network.GSON.JSONBlock;
import Network.GSON.JSONClass;
import robin.Block;
import robin.Message;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class UserConnection
{
    private String host;
    private int port;
    private PrintStream os;
    private BufferedReader is;
    private Socket nodeSocket;

    public UserConnection(String host, int port)
    {
        this.host = host;
        this.port = port;
    }

    public void start() throws IOException
    {
        nodeSocket = new Socket(host, port);
        this.os = new PrintStream(nodeSocket.getOutputStream());
        this.is = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));
    }
    public void close() throws IOException
    {
        nodeSocket.close();
        is.close();
        os.close();
    }
    public void sendMsg (Message message) throws IOException
    {
        JSONClass json = new JSONClass(message);
        os.println(json.makeJSONMessageString());
    }
    public int requestBlockIndex ()
    {
        String blockIndexStr;
        int blockIndex = 0;
        JSONClass json = new JSONClass();
        os.println(json.makeJSONBlockIndexString());
        try {
            while (true) {
                blockIndexStr = is.readLine();

                if (!blockIndexStr.isEmpty() ) {
                    blockIndex = Integer.parseInt(blockIndexStr);
                    break;
                }
            }
            is.close();
            os.close();
        }
        catch (IOException e ) {e.printStackTrace();}
        return blockIndex;
    }
    public ArrayList<Block> requestBlocksByIndex(long currentIndex)
    {
        ArrayList<Block> blocks = new ArrayList<>();
        JSONClass json = new JSONClass(currentIndex);
        os.println(json.makeJSONBlocksByIndexString());
        String blockStr;

        try {
            while ((blockStr = is.readLine()) != null) {
                if (blockStr.equals(".done"))
                    break;
                if (!blockStr.isEmpty()) {
                    blocks.add(new JSONClass(blockStr).getJSONBlock().getBlock());
                }
            }
            is.close();
            os.close();
        }
        catch (IOException e ) {e.printStackTrace();}

        return blocks;
    }
    public boolean isConnected() { return nodeSocket.isConnected(); }
}
