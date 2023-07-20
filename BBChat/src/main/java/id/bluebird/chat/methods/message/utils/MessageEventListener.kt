package id.bluebird.chat.methods.message.utils

import id.bluebird.chat.methods.message.MessageActivity
import id.bluebird.chat.sdk.UiUtils

class MessageEventListener constructor(val activity: MessageActivity, online: Boolean) :
    UiUtils.EventListener(activity, online) {

    override fun onLogin(code: Int, txt: String) {
        super.onLogin(code, txt)
        UiUtils.attachMeTopic(activity, null)
        activity.topicAttach()
    }
}