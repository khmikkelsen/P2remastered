package Network.node;

import Network.GSON.JSONClass;
import robin.Block;

import java.io.*;
import java.net.Socket;

public class NodeConnection
{
    private String host;
    private int port;
    private PrintStream os;
    private BufferedReader is;
    private Socket nodeSocket;

    public NodeConnection(String host, int port)
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
    public void sendBlock(Block block)
    {
        JSONClass json = new JSONClass(block);
        os.println(json.makeJSONBlockString());
    }
}
