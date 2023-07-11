package id.bluebird.chat

import android.app.Activity
import androidx.compose.runtime.MutableState
import id.bluebird.chat.sdk.db.BaseDb
import id.bluebird.chat.methods.login as BBChatLogin
import id.bluebird.chat.methods.toMessageScreen as BBChatToMessageScreen
import id.bluebird.chat.methods.toCallScreen as BBChatToCallScreen
import id.bluebird.chat.methods.logout as BBChatLogout

class BBChat {

    companion object {
        fun login(
            activity: Activity,
            isLoginSuccess: MutableState<Boolean>,
            isLoading: MutableState<Boolean>
        ) = BBChatLogin(activity, isLoginSuccess, isLoading)

        fun toMessageScreen(
            context: Activity,
            topicName: String
        ) = BBChatToMessageScreen(context, topicName)

        fun toCallScreen(
            context: Activity,
            topicName: String
        ) = BBChatToCallScreen(context, topicName)

        fun logout(): Boolean = BBChatLogout()
    }
}