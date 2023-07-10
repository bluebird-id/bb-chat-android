package id.bluebird.chat.methods

import android.app.Activity
import android.content.Intent
import id.bluebird.chat.sdk.Const
import id.bluebird.chat.sdk.demos.message.MessageActivity

fun toMessageScreen(context: Activity) {
    val launch = Intent(context, MessageActivity::class.java)
    launch.putExtra(Const.INTENT_EXTRA_TOPIC, "usr7yG--GVH87o")
    context.startActivity(launch)
}
