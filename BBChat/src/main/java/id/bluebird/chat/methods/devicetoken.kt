package id.bluebird.chat.methods

import android.util.Log
import id.bluebird.chat.io.ChatServiceApi
import id.bluebird.chat.io.ChatServiceRepositoryImpl
import id.bluebird.chat.io.network.Result
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun saveDeviceToken(
    appId: Long,
    deviceToken: String,
    participantId: String,
    onSuccess: (result: String?) -> Unit,
    onError: (result: String?) -> Unit
) {
    Log.e("BBChat", "SaveDeviceToken , token = $deviceToken")

    GlobalScope.launch {

        val repository = ChatServiceRepositoryImpl(ChatServiceApi(null))
        when (val response = repository.saveDeviceToken(appId, participantId, deviceToken)) {
            is Result.Ok -> {
                onSuccess.invoke("SaveDeviceToken success ${response.data.message}")
            }

            is Result.Exception -> {
                onError.invoke("SaveDeviceToken failed ${response.errorMessage}")
            }

            else -> {
                onError.invoke("SaveDeviceToken failed Network")
            }
        }
    }

}