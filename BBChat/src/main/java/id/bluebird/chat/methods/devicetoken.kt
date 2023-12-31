package id.bluebird.chat.methods

import android.util.Log
import co.tinode.tinodesdk.PromisedReply
import co.tinode.tinodesdk.ServerResponseException
import co.tinode.tinodesdk.model.ServerMessage
import id.bluebird.chat.NotifPipeline
import id.bluebird.chat.Platform
import id.bluebird.chat.io.ChatServiceApi
import id.bluebird.chat.io.ChatServiceRepositoryImpl
import id.bluebird.chat.io.network.Result
import id.bluebird.chat.sdk.Cache
import id.bluebird.chat.sdk.UiUtils
import id.bluebird.chat.sdk.media.VxCard
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


fun saveDeviceToken(
    clientId: String,
    deviceToken: String,
    platform: Platform,
    notifPipline: NotifPipeline,
    onSuccess: ((result: String?) -> Unit)?,
    onError: ((result: String?) -> Unit)?
) {
    Log.e("BBChat", "SaveDeviceToken , token = $deviceToken ${platform.value} ${notifPipline.value}" )

    val tinode = Cache.getTinode()
    if (tinode.myId == null) {
        onError?.invoke("tinode id not found")
        return
    }
    GlobalScope.launch {

        val repository = ChatServiceRepositoryImpl(ChatServiceApi(null))
        when (val response = repository.saveDeviceToken(clientId, tinode.myId, deviceToken, platform, notifPipline)) {
            is Result.Ok -> {
                onSuccess?.invoke("SaveDeviceToken success ${response.data.message}")
            }

            is Result.Exception -> {
                onError?.invoke("SaveDeviceToken failed ${response.errorMessage}")
            }

            else -> {
                onError?.invoke("SaveDeviceToken failed Network")
            }
        }
    }

    setNotifClientIdTinode(clientId)
}

private fun setNotifClientIdTinode(notifClientId: String) {
    val me = Cache.getTinode().getMeTopic<VxCard>() ?: return

    UiUtils.updateTopicDesc(me, notifClientId)
        .thenApply(object : PromisedReply.SuccessListener<ServerMessage<*, *, *, *>?>() {

            override fun onSuccess(result: ServerMessage<*, *, *, *>?): PromisedReply<ServerMessage<*, *, *, *>?>? {

                return null
            }
        })
        .thenCatch(object : PromisedReply.FailureListener<ServerMessage<*, *, *, *>?>() {

            override fun <E : java.lang.Exception?> onFailure(err: E): PromisedReply<ServerMessage<*, *, *, *>?>? {

                val errMessage = (err as ServerResponseException).reason
                Log.e("BBChat", errMessage)
                return null
            }

        })

}