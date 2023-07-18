package id.bluebird.chat.methods

import android.app.Activity
import android.util.Log
import androidx.preference.PreferenceManager
import co.tinode.tinodesdk.PromisedReply
import co.tinode.tinodesdk.PromisedReply.FailureListener
import co.tinode.tinodesdk.ServerResponseException
import co.tinode.tinodesdk.model.AuthScheme
import co.tinode.tinodesdk.model.Credential
import co.tinode.tinodesdk.model.MetaSetDesc
import co.tinode.tinodesdk.model.ServerMessage
import id.bluebird.chat.R
import id.bluebird.chat.sdk.AttachmentHandler
import id.bluebird.chat.sdk.Cache
import id.bluebird.chat.sdk.MyAttachmentHandler
import id.bluebird.chat.sdk.TindroidApp
import id.bluebird.chat.sdk.UiUtils
import id.bluebird.chat.sdk.account.Utils
import id.bluebird.chat.sdk.media.VxCard
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private var userName: String? = null
private var passWord: String? = null

fun loginOrRegister(
    username: String,
    activity: Activity,
    onSuccess: (result: String?) -> Unit,
    onError: (result: String?) -> Unit
) {
    userName = username
    passWord = username

    GlobalScope.launch {
        loginTinode(activity, completion = { result, error ->
            if (result) {
                onSuccess.invoke("Login Success")
            } else {
                onError.invoke(error)
            }
        })
    }

}

private suspend fun loginTinode(
    activity: Activity,
    completion: (Boolean, String) -> Unit
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
                        completion.invoke(false, message + " ${msg.ctrl.code}")

                    } else {

                        tinode.setAutoLoginToken(tinode.authToken)
                        completion.invoke(true, "Login Success")

                    }

                    return null
                }
            })
        .thenCatch(
            object : FailureListener<ServerMessage<*, *, *, *>?>() {
                override fun <E : Exception?> onFailure(err: E):
                        PromisedReply<ServerMessage<*, *, *, *>?>? {

                    val message: String = activity.getString(R.string.error_login_failed)
                    val errMessage = if (err != null) {
                        " -> " + message + err.message
                    } else {
                        ""
                    }

                    if (err?.message?.contains("401") == true) {
                        registerTinode(activity, completion)
                    } else {
                        completion.invoke(false, errMessage)
                    }
                    return null
                }
            })
}

private fun registerTinode(
    activity: Activity,
    completion: (Boolean, String) -> Unit
) {
    // This is called on the websocket thread.
    val tinode = Cache.getTinode()
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
    val hostName = sharedPref.getString(Utils.PREFS_HOST_NAME, TindroidApp.getDefaultHostName())
    val tls = sharedPref.getBoolean(Utils.PREFS_USE_TLS, TindroidApp.getDefaultTLS())

    val theCard = VxCard(userName, "")

    val credentials = ArrayList<Credential>()

    // This is called on the websocket thread.

    tinode.connect(hostName, tls, false)

        .thenApply(object : PromisedReply.SuccessListener<ServerMessage<*, *, *, *>?>() {

            override fun onSuccess(result: ServerMessage<*, *, *, *>?): PromisedReply<ServerMessage<*, *, *, *>?>? {
                return MyAttachmentHandler.uploadAvatar(theCard, null, "newacc")
            }

        })
        .thenApply(object : PromisedReply.SuccessListener<ServerMessage<*, *, *, *>?>() {

            override fun onSuccess(result: ServerMessage<*, *, *, *>?): PromisedReply<ServerMessage<*, *, *, *>?>? {

                val meta = MetaSetDesc<VxCard, String?>(theCard, null)
                return tinode.createAccountBasic<VxCard, String?>(
                    userName, passWord, true, null, meta,
                    credentials.toArray(arrayOf<Credential>())
                )
            }


        })
        .thenApply(object : PromisedReply.SuccessListener<ServerMessage<*, *, *, *>?>() {

            override fun onSuccess(result: ServerMessage<*, *, *, *>?): PromisedReply<ServerMessage<*, *, *, *>?>? {
                UiUtils.updateAndroidAccount(
                    activity, tinode.myId,
                    AuthScheme.basicInstance(userName, passWord).toString(),
                    tinode.authToken, tinode.authTokenExpiration
                )

                // Flip back to login screen on success;
                if ((result?.ctrl?.code
                        ?: 0) >= 300 && result?.ctrl?.text?.contains("validate credentials") == true
                ) {

                    Log.e("BBChat", "Register Failed ${result.ctrl.code}")
                    completion.invoke(false, "Register Failed ${result.ctrl.code}")

                } else {

                    tinode.setAutoLoginToken(tinode.authToken)
                    Log.e("BBChat", "Register Success")
                    completion.invoke(true, "Register Success")
                    registerChatService(activity, completion, tinode.myId)
                }
                return null
            }
        })
        .thenCatch(object : FailureListener<ServerMessage<*, *, *, *>?>() {

            override fun <E : java.lang.Exception?> onFailure(err: E): PromisedReply<ServerMessage<*, *, *, *>?>? {

                val errMessage = if (err is ServerResponseException) {
                    when ((err as ServerResponseException).reason) {
                        "auth" -> "Invalid Login"
                        "email" -> "Duplicate Email"
                        else -> "Register Failed Unknown Reason"
                    }
                } else {
                    "Failed create account"
                }

                Log.e("BBChat", errMessage)
                completion.invoke(false, errMessage)
                return null
            }


        })
}

private fun registerChatService(
    activity: Activity,
    completion: (Boolean, String) -> Unit,
    tinodeId: String
) {
    Log.e("BBChat", "tinode id = $tinodeId")
    completion.invoke(true, "tinode id = $tinodeId")
}

