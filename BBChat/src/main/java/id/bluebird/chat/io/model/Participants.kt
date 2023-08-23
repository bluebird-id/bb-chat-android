package id.bluebird.chat.io.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
class Participants(
    @SerializedName("participants_id_list")
    val participantsIdlist: List<String> = listOf(),
    @SerializedName("room_id")
    val roomId: String = "",
) : Serializable
