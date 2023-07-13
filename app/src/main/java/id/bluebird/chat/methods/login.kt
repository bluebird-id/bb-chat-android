package id.bluebird.chat.methods

import android.app.Activity
import android.util.Log
import androidx.preference.PreferenceManager
import co.tinode.tinodesdk.PromisedReply
import co.tinode.tinodesdk.model.AuthScheme
import co.tinode.tinodesdk.model.ServerMessage
import id.bluebird.chat.R
import id.bluebird.chat.sdk.Cache
import id.bluebird.chat.sdk.TindroidApp
import id.bluebird.chat.sdk.UiUtils
import id.bluebird.chat.sdk.account.Utils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private var userName: String? = null
private var passWord: String? = null

fun loginOrRegister(
    username: String, password: String,
    activity: Activity,
    onSuccess: (result: String?) -> Unit,
    onError: (result: String?) -> Unit
) {
    userName = username
    passWord = password

    GlobalScope.launch {
        loginTinode(activity, result = { result, error ->
            if (result) {
                onSuccess.invoke("Login Success")
            } else {
                onError.invoke(error)
            }
        })
    }

}

private fun loginTinode(
    activity: Activity,
    result: (Boolean, String) -> Unit
) {
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
    val tinode = Cache.getTinode()

    val hostName: String =
        sharedPref.getString(Utils.PREFS_HOST_NAME, TindroidApp.getDefaultHostName())!!
    val tls: Boolean = sharedPref.getBoolean(Utils.PREFS_USE_TLS, TindroidApp.getDefaultTLS())

    Log.e("BBChat", "loginTinode: $userName $passWord")
    // This is called on the websocket thread.
    tinode.connect(hostName, tls, false)
        .thenApply(
            object : PromisedReply.SuccessListener<ServerMessage<*, *, *, *>?>() {
                override fun onSuccess(result: ServerMessage<*, *, *, *>?):
                        PromisedReply<ServerMessage<*, *, *, *>?> =
                    tinode.loginBasic(userName, passWord)
            })
        .thenApply(
            object : PromisedReply.SuccessListener<ServerMessage<*, *, *, *>?>() {
                override fun onSuccess(msg: ServerMessage<*, *, *, *>?):
                        PromisedReply<ServerMessage<*, *, *, *>?>? {
                    UiUtils.updateAndroidAccount(
                        activity,
                        tinode.myId,
                        AuthScheme.basicInstance(userName, passWord).toString(),
                        tinode.authToken,
                        tinode.authTokenExpiration
                    )

                    // msg could be null if earlier login has succeeded.
                    if (msg != null && msg.ctrl.code >= 300 &&
                        msg.ctrl.text.contains("validate credentials")
                    ) {

                        val message: String = activity.getString(R.string.error_login_failed)
                        result.invoke(false, message + "${msg.ctrl.code}")

                    } else {

                        tinode.setAutoLoginToken(tinode.authToken)
                        result.invoke(true, "")

                    }

                    return null
                }
            })
        .thenCatch(
            object : PromisedReply.FailureListener<ServerMessage<*, *, *, *>?>() {
                override fun <E : Exception?> onFailure(err: E):
                        PromisedReply<ServerMessage<*, *, *, *>?>? {

                    val message: String = activity.getString(R.string.error_login_failed)
                    val errMessage = if (err != null) { message + err.message } else { "" }

                    result.invoke(false, errMessage)
                    return null
                }
            })
}

private fun registerTinode() {

}

private fun registerChatService() {

}

