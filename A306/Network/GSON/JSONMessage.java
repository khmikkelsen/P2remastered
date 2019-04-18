package Network.GSON;

import robin.Message;
import static Network.GSON.Command.*;

public class JSONMessage
{
    private Command command = SEND_MESSAGE;
    private Message message;

    public JSONMessage(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
