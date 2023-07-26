package id.bluebird.chat

import android.app.Activity
import id.bluebird.chat.methods.loginOrRegister as BBChatLogin
import id.bluebird.chat.methods.message.toMessageScreen as BBChatToMessageScreen
import id.bluebird.chat.methods.toCallScreen as BBChatToCallScreen
import id.bluebird.chat.methods.logout as BBChatLogout
import id.bluebird.chat.methods.getRoomByOrderId as BBChatGetRoom

class BBChat {

    companion object {
        fun login(
            username: String,
            activity: Activity,
            onSuccess: (result: String?) -> Unit,
            onError: (result: String?) -> Unit
        ) = BBChatLogin(username, activity, onSuccess, onError)

        fun getRoom(
            context: Activity,
            orderId: String,
            onSuccess: (result: String?) -> Unit,
            onError: (result: String?) -> Unit
        ) = BBChatGetRoom(context, orderId, onSuccess, onError)

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