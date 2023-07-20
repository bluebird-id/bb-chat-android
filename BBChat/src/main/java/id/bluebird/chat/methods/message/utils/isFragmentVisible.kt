package id.bluebird.chat.methods.message.utils

import id.bluebird.chat.methods.message.MessageActivity

fun MessageActivity.isFragmentVisible(tag: String): Boolean {
    val fragment = supportFragmentManager.findFragmentByTag(tag)
    return fragment != null && fragment.isVisible
}