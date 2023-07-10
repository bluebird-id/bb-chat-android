package id.bluebird.chat.methods

import android.app.Activity
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.preference.PreferenceManager
import co.tinode.tinodesdk.PromisedReply
import co.tinode.tinodesdk.model.AuthScheme
import co.tinode.tinodesdk.model.ServerMessage
import id.bluebird.chat.R
import id.bluebird.chat.sdk.Cache
import id.bluebird.chat.sdk.TindroidApp
import id.bluebird.chat.sdk.UiUtils
import id.bluebird.chat.sdk.account.Utils

fun login(
    activity: Activity,
    isLoginSuccess: MutableState<Boolean>,
    isLoading: MutableState<Boolean>
) {
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
    val tinode = Cache.getTinode()

    val hostName: String =
        sharedPref.getString(Utils.PREFS_HOST_NAME, TindroidApp.getDefaultHostName())!!
    val tls: Boolean = sharedPref.getBoolean(Utils.PREFS_USE_TLS, TindroidApp.getDefaultTLS())
    val username = "customer"
    val password = username

    isLoading.value = true

    // This is called on the websocket thread.
    tinode.connect(hostName, tls, false)
        .thenApply(
            object : PromisedReply.SuccessListener<ServerMessage<*, *, *, *>?>() {
                override fun onSuccess(result: ServerMessage<*, *, *, *>?):
                        PromisedReply<ServerMessage<*, *, *, *>?> =
                    tinode.loginBasic(username, password)
            })
        .thenApply(
            object : PromisedReply.SuccessListener<ServerMessage<*, *, *, *>?>() {
                override fun onSuccess(msg: ServerMessage<*, *, *, *>?):
                        PromisedReply<ServerMessage<*, *, *, *>?>? {
                    UiUtils.updateAndroidAccount(
                        activity,
                        tinode.myId,
                        AuthScheme.basicInstance(username, password).toString(),
                        tinode.authToken,
                        tinode.authTokenExpiration
                    )

                    // msg could be null if earlier login has succeeded.
                    if (msg != null && msg.ctrl.code >= 300 &&
                        msg.ctrl.text.contains("validate credentials")
                    ) {
                        val message: String = activity.getString(R.string.error_login_failed)

                        activity.runOnUiThread {
                            Toast.makeText(
                                activity,
                                message + "${msg.ctrl.code}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        tinode.setAutoLoginToken(tinode.authToken)

                        isLoginSuccess.value = true

                        activity.runOnUiThread {
                            Toast.makeText(activity, "Login Success", Toast.LENGTH_SHORT).show()
                        }
                    }

                    isLoading.value = false
                    return null
                }
            })
        .thenCatch(
            object : PromisedReply.FailureListener<ServerMessage<*, *, *, *>?>() {
                override fun <E : Exception?> onFailure(err: E):
                        PromisedReply<ServerMessage<*, *, *, *>?>? {

                    val message: String = activity.getString(R.string.error_login_failed)
                    val errMessage = if (err != null) {
                        message + err.message
                    } else {
                        ""
                    }

                    isLoading.value = false

                    activity.runOnUiThread {
                        Toast.makeText(activity, errMessage, Toast.LENGTH_SHORT).show()
                    }
                    return null
                }
            })
}
