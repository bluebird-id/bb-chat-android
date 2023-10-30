package id.bluebird.chat.methods

import android.app.Activity
import android.content.Context
import id.bluebird.chat.sdk.CallManager

fun toCallScreen(context: Context, topicName: String) {
    CallManager.placeOutgoingCall(context, topicName)
}

