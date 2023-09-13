package id.bluebird.chat.methods

import android.util.Log
import id.bluebird.chat.io.ChatServiceApi
import id.bluebird.chat.io.ChatServiceRepositoryImpl
import id.bluebird.chat.io.model.Participants
import id.bluebird.chat.io.network.Result
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun getRoomByOrderId(
    userId: String,
    orderId: String,
    onSuccess: (result: Participants?) -> Unit,
    onError: (result: String?) -> Unit
) {
    Log.e("BBChat", "GetParticipant , orderid = $orderId")

    GlobalScope.launch {

        val repository = ChatServiceRepositoryImpl(ChatServiceApi(userId))
        when (val response = repository.getParticipants(orderId)) {
            is Result.Ok -> {
                onSuccess.invoke(response.data.participants)
            }

            is Result.Exception -> {
                onError.invoke("GetParticipant failed ${response.errorMessage}")
            }

            else -> {
                onError.invoke("GetParticipant failed Network")
            }
        }
    }

}