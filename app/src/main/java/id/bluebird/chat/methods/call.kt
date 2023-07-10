package id.bluebird.chat.methods

import android.app.Activity
import id.bluebird.chat.sdk.CallManager

fun toCallScreen(context: Activity, topicName: String) {
    CallManager.placeOutgoingCall(context, topicName)
}
