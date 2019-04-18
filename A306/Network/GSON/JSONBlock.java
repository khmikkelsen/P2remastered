package Network.GSON;

import robin.Block;

import static Network.GSON.Command.*;

public class JSONBlock
{
    private Command command = BLOCK_MINED;
    private Block block;

    public JSONBlock(Block block)
    {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
