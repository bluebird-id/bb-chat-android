package id.bluebird.chat.methods.message

import android.app.Activity
import android.content.Intent
import id.bluebird.chat.sdk.Cache
import id.bluebird.chat.sdk.Const

fun toMessageScreen(context: Activity, topicName: String) {
    val tinode = Cache.getTinode()
    val launch = Intent(context, MessageActivity::class.java)
    launch.putExtra(Const.INTENT_EXTRA_TOPIC, topicName)
    launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(launch)
}
