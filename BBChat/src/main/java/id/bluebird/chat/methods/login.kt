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
import id.bluebird.chat.io.ChatServiceApi
import id.bluebird.chat.io.ChatServiceRepositoryImpl
import id.bluebird.chat.io.network.Result
import id.bluebird.chat.sdk.Cache
import id.bluebird.chat.sdk.MyAttachmentHandler
import id.bluebird.chat.sdk.UiUtils
import id.bluebird.chat.sdk.account.Utils
import id.bluebird.chat.sdk.app.TindroidApp
import id.bluebird.chat.sdk.media.VxCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


val coroutineScope = CoroutineScope(Dispatchers.IO)

private var userName: String? = null
private var passWord: String? = null
private var fullName: String? = null

fun loginOrRegister(
    username: String,
    fullname: String,
    activity: Activity,
    onSuccess: (result: String?) -> Unit,
    onError: (result: String?) -> Unit
) {
    userName = username
    passWord = username
    fullName = fullname

    coroutineScope.launch {

        // LOGIN TINODE
        loginTinode(activity, completion = { result, error ->

            if (result?.isNotEmpty() == true) {

                // LOGIN TINODE SUCCESS
                // LOGIN OR REGISTER CHAT SERVICE
                loginOrRegisterChatService(result, completion = { _, _ ->
                    onSuccess.invoke("Login Success")
                })

            } else {

                if (error?.contains("401") == true) {

                    // LOGIN TINODE ACCOUNT NOT FOUND
                    registerTinode(activity, completion = { result, error ->

                        if (result?.isNotEmpty() == true) {

                            loginOrRegisterChatService(result, completion = { _, _ ->
                                onSuccess.invoke("Login Success")
                            })

                        } else {
                            onError.invoke(error)
                        }
                    })

                } else {

                    // LOGIN TINODE SERVER ERROR
                    onError.invoke(error)

                }

            }
        })


    }

}

private fun loginTinode(
    activity: Activity,
    completion: (String?, String?) -> Unit
) {
    Log.e("BBChat", "loginTinode: $userName $passWord")
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
    val tinode = Cache.getTinode()

    val hostName: String =
        sharedPref.getString(Utils.PREFS_HOST_NAME, TindroidApp.getDefaultHostName())!!
    val tls: Boolean = sharedPref.getBoolean(Utils.PREFS_USE_TLS, TindroidApp.getDefaultTLS())

    Log.e("BBChat", "loginTinode: host:$hostName $userName $passWord")
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
                        completion.invoke(null, message + " ${msg.ctrl.code}")

                    } else {

                        tinode.setAutoLoginToken(tinode.authToken)

                        //For fetch topic list from bb-tinode server who attach to user
                        Cache.attachMeTopic(null)

                        completion.invoke(tinode.myId, null)

                    }

                    return null
                }
            })
        .thenCatch(
            object : FailureListener<ServerMessage<*, *, *, *>?>() {
                override fun <E : Exception?> onFailure(err: E):
                        PromisedReply<ServerMessage<*, *, *, *>?>? {

                    val message: String = activity.getString(R.string.error_login_failed)
                    completion.invoke(null, message + " " + err?.message)

                    return null
                }
            })
}

private fun registerTinode(
    activity: Activity,
    completion: (String?, String?) -> Unit
) {
    // This is called on the websocket thread.

    Log.e("BBChat", "register tinode: $userName $passWord")
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

                    completion.invoke(null, "Register Failed ${result.ctrl.code}")

                } else {

                    tinode.setAutoLoginToken(tinode.authToken)
                    completion.invoke(tinode.myId, "Register Success")
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

                completion.invoke(null, errMessage)
                return null
            }


        })
}

private fun loginOrRegisterChatService(
    tinodeId: String,
    completion: (String?, String?) -> Unit
) {
    Log.e("BBChat", "login or register chat service: $userName $passWord")
    coroutineScope.launch {
        val clientId = userName ?: ""

        val repository = ChatServiceRepositoryImpl(ChatServiceApi(null))
        when (val response = repository.register(clientId, tinodeId, fullName ?: "")) {
            is Result.Ok -> {
                completion.invoke("register success ${response.data.message}", null)
            }

            is Result.Exception -> {
                completion.invoke(null, "register failed ${response.errorMessage}")
            }
        }
    }
}

