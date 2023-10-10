package id.bluebird.chat.io.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
class Participants(
    @SerializedName("call_room_id")
    val callRoomId: String = "",
    @SerializedName("chat_room_id")
    val chatRoomId: String = "",
    @SerializedName("full_name")
    val fullName: String = "",
) : Serializable
