package Network.GSON;

import RSA.RSAKey;
import RSA.Signature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import Network.GSON.Serializers.RSAKeyDeserializer;
import Network.GSON.Serializers.RSAKeySerializer;
import Network.GSON.Serializers.SignatureDeserializer;
import Network.GSON.Serializers.SignatureSerializer;
import robin.Block;
import robin.Message;

public class JSONClass
{
    private GsonBuilder gsonBuilder;
    private Gson gson;
    private NetworkCommand command;
    private String json;
    private Message message;
    private Block block;
    private long index;

    public JSONClass(String json) {

        gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(RSAKey.class, new RSAKeySerializer());
        gsonBuilder.registerTypeAdapter(RSAKey.class, new RSAKeyDeserializer());
        gsonBuilder.registerTypeAdapter(Signature.class, new SignatureSerializer());
        gsonBuilder.registerTypeAdapter(Signature.class, new SignatureDeserializer());
        gson = gsonBuilder.create();
        this.json = json;
    }
    public JSONClass(Message message) {

        gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(RSAKey.class, new RSAKeySerializer());
        gsonBuilder.registerTypeAdapter(RSAKey.class, new RSAKeyDeserializer());
        gsonBuilder.registerTypeAdapter(Signature.class, new SignatureSerializer());
        gsonBuilder.registerTypeAdapter(Signature.class, new SignatureDeserializer());
        gson = gsonBuilder.create();
        this.message = message;
    }
    public JSONClass(Block block) {

        gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(RSAKey.class, new RSAKeySerializer());
        gsonBuilder.registerTypeAdapter(RSAKey.class, new RSAKeyDeserializer());
        gsonBuilder.registerTypeAdapter(Signature.class, new SignatureSerializer());
        gsonBuilder.registerTypeAdapter(Signature.class, new SignatureDeserializer());
        gson = gsonBuilder.create();
        this.block = block;
    }
    public JSONClass(long index) {

        gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(RSAKey.class, new RSAKeySerializer());
        gsonBuilder.registerTypeAdapter(RSAKey.class, new RSAKeyDeserializer());
        gsonBuilder.registerTypeAdapter(Signature.class, new SignatureSerializer());
        gsonBuilder.registerTypeAdapter(Signature.class, new SignatureDeserializer());
        gson = gsonBuilder.create();
        this.index = index;
    }
    public JSONClass() {
        gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(RSAKey.class, new RSAKeySerializer());
        gsonBuilder.registerTypeAdapter(RSAKey.class, new RSAKeyDeserializer());
        gsonBuilder.registerTypeAdapter(Signature.class, new SignatureSerializer());
        gsonBuilder.registerTypeAdapter(Signature.class, new SignatureDeserializer());
        gson = gsonBuilder.create();
    }

    public NetworkCommand getCommand ()
    {
        return gson.fromJson(json, NetworkCommand.class);
    }
    public JSONMessage getJSONMessage()
    {
        return gson.fromJson(json, JSONMessage.class);
    }
    public JSONBlock getJSONBlock () { return gson.fromJson(json, JSONBlock.class); }
    public JSONBlocksByIndex getJSONBlocksByIndex () { return gson.fromJson(json, JSONBlocksByIndex.class);}

    public String makeJSONMessageString()
    {
        return gson.toJson(new JSONMessage(message), JSONMessage.class);
    }
    public String makeJSONBlockIndexString () { return gson.toJson(new JSONBlockIndex(), JSONBlockIndex.class); }
    public String makeJSONBlockString () { return gson.toJson(new JSONBlock(block), JSONBlock.class); }
    public String makeJSONBlocksByIndexString () { return gson.toJson(new JSONBlocksByIndex(index), JSONBlocksByIndex.class); }
}
