package id.bluebird.chat.methods

import android.app.Activity
import android.util.Log
import id.bluebird.chat.io.ChatServiceApi
import id.bluebird.chat.io.ChatServiceRepositoryImpl
import id.bluebird.chat.io.network.Result
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun getRoomByOrderId(
    activity: Activity,
    orderId: String,
    onSuccess: (result: String?) -> Unit,
    onError: (result: String?) -> Unit
) {
    Log.e("BBChat", "GetParticipant , orderid = $orderId")

    GlobalScope.launch {

        val repository = ChatServiceRepositoryImpl(ChatServiceApi())
        when (val response = repository.getParticipants(orderId)) {
            is Result.Ok -> {
                onSuccess.invoke(response.data.participants.roomId)
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