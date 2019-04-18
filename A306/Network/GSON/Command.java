package Network.GSON;

import com.google.gson.annotations.SerializedName;

public enum Command {
    @SerializedName("sendmessage") SEND_MESSAGE,
    @SerializedName("getblockcount") GET_BLOCK_COUNT,
    @SerializedName("getblock") GET_BLOCK_BY_INDEX,
    @SerializedName("blockmined") BLOCK_MINED,
    UNKNOWN
}
