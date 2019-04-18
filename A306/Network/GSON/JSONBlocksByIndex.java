package Network.GSON;

import static Network.GSON.Command.*;

public class JSONBlocksByIndex
{
    private Command command = GET_BLOCK_BY_INDEX;
    private long index;

    public JSONBlocksByIndex(long index)
    {
        this.index = index;
    }

    public long getIndex() { return index; }
}
