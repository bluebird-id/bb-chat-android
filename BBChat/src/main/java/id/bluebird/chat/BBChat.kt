package id.bluebird.chat

import android.app.Activity
import id.bluebird.chat.io.model.Participants
import id.bluebird.chat.methods.getRoomByOrderId as BBChatGetRoom
import id.bluebird.chat.methods.loginOrRegister as BBChatLogin
import id.bluebird.chat.methods.logout as BBChatLogout
import id.bluebird.chat.methods.message.toMessageScreen as BBChatToMessageScreen
import id.bluebird.chat.methods.toCallScreen as BBChatToCallScreen
import id.bluebird.chat.methods.saveDeviceToken as BBChatSaveDeviceToken

class BBChat {

    companion object {
        fun login(
            username: String,
            fullname: String,
            activity: Activity,
            onSuccess: (result: String?) -> Unit,
            onError: (result: String?) -> Unit
        ) = BBChatLogin(username, fullname, activity, onSuccess, onError)

        fun getRoom(
            userId: String,
            orderId: String,
            onSuccess: (result: Participants?) -> Unit,
            onError: (result: String?) -> Unit
        ) = BBChatGetRoom(userId, orderId, onSuccess, onError)

        fun toMessageScreen(
            context: Activity,
            otherName: String,
            topicChatName: String,
            topicCallName: String,
        ) = BBChatToMessageScreen(context, otherName, topicChatName, topicCallName)

        fun toCallScreen(
            context: Activity,
            topicName: String
        ) = BBChatToCallScreen(context, topicName)

        fun saveDeviceToken(
            appId: Long,
            deviceToken: String,
            participantId: String,
            onSuccess: (result: String?) -> Unit,
            onError: (result: String?) -> Unit
        ) = BBChatSaveDeviceToken(appId, deviceToken, participantId, onSuccess, onError)

        fun logout(): Boolean = BBChatLogout()
    }
}