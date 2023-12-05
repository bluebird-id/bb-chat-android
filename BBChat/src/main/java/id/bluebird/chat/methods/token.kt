package id.bluebird.chat.methods

import android.util.Log
import id.bluebird.chat.io.ChatServiceApi
import id.bluebird.chat.io.ChatServiceRepositoryImpl
import id.bluebird.chat.io.network.Result
import id.bluebird.chat.sdk.Cache
import kotlinx.coroutines.launch

fun generateNewToken(clientId: String, clientSecret: String) {
    Log.e("BBChat", "generate new token: $clientId $clientSecret")
    coroutineScope.launch {

        val repository = ChatServiceRepositoryImpl(ChatServiceApi(null))
        when (val response = repository.generateNewToken(clientId, clientSecret)) {
            is Result.Ok -> {
                Log.e("BBChat", "token: ${response.data.token}")
                Cache.setToken(response.data.token)
            }

            is Result.Exception -> {
                Log.e("BBChat", "token error: ${response.errorMessage}")
            }
        }
    }
}

fun refreshToken() {

}