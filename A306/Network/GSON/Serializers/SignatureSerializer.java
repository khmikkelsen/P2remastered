package Network.GSON.Serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import RSA.Signature;

import java.lang.reflect.Type;

public class SignatureSerializer implements JsonSerializer<Signature> {
    @Override
    public JsonElement serialize(Signature src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getBase64String());
    }
}



