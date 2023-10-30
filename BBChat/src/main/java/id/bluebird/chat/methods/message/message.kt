package id.bluebird.chat.methods.message

import android.app.Activity
import android.content.Context
import android.content.Intent
import id.bluebird.chat.sdk.Const

fun toMessageScreen(
    context: Context,
    otherName: String,
    chatTopicName: String,
    callTopicName: String,
) {
    val launch = Intent(context, MessageActivity::class.java)
    launch.putExtra(Const.INTENT_EXTRA_OTHER_NAME_CHAT, otherName)
    launch.putExtra(Const.INTENT_EXTRA_TOPIC_CHAT, chatTopicName)
    launch.putExtra(Const.INTENT_EXTRA_TOPIC_CALL, callTopicName)
    launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(launch)
}
