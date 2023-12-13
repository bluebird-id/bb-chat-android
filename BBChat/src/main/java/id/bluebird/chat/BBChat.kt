package id.bluebird.chat

import android.content.Context
import id.bluebird.chat.io.model.Participants
import id.bluebird.chat.methods.message.MessageActivity
import id.bluebird.chat.sdk.demos.message.UserType
import id.bluebird.chat.methods.generateNewToken as BBChatNewToken
import id.bluebird.chat.methods.getRoomByOrderId as BBChatGetRoom
import id.bluebird.chat.methods.loginOrRegister as BBChatLogin
import id.bluebird.chat.methods.logout as BBChatLogout
import id.bluebird.chat.methods.message.toMessageScreen as BBChatToMessageScreen
import id.bluebird.chat.methods.saveDeviceToken as BBChatSaveDeviceToken
import id.bluebird.chat.methods.toCallScreen as BBChatToCallScreen

class BBChat {

    companion object {
        var CHAT_ENABLED = false

        fun generateNewToken(
            clientId: String,
            clientSecret: String,
        ) = BBChatNewToken(clientId, clientSecret)

        fun login(
            username: String,
            fullname: String,
            onSuccess: (result: String?) -> Unit,
            onError: (result: String?) -> Unit,
        ) = BBChatLogin(username, fullname, onSuccess, onError)

        fun getRoom(
            userId: String,
            orderId: String,
            onSuccess: (result: Participants?) -> Unit,
            onError: (result: String?) -> Unit
        ) = BBChatGetRoom(userId, orderId, onSuccess, onError)

        fun toMessageScreen(
            context: Context,
            opponentsName: String,
            topicChatName: String,
            topicCallName: String,
            userType: UserType,
        ) = BBChatToMessageScreen(context, opponentsName, topicChatName, topicCallName, userType)

        fun toCallScreen(
            context: Context,
            topicName: String
        ) = BBChatToCallScreen(context, topicName)

        @JvmStatic
        fun saveDeviceToken(
            clientId: String,
            deviceToken: String,
            platform: Platform,
            notifPipeline: NotifPipeline,
            onSuccess: ((result: String?) -> Unit)?,
            onError: ((result: String?) -> Unit)?
        ) = BBChatSaveDeviceToken(
            clientId,
            deviceToken,
            platform,
            notifPipeline,
            onSuccess,
            onError
        )

        fun logout(): Boolean = BBChatLogout()

        fun enableChat(value: Boolean): Unit {
            CHAT_ENABLED = value
            MessageActivity.setChatEnabled(value)
        }
    }
}