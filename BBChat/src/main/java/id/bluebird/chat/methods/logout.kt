package id.bluebird.chat.methods

import id.bluebird.chat.sdk.Cache
import id.bluebird.chat.sdk.CallManager
import id.bluebird.chat.sdk.app.TindroidApp

fun logout(): Boolean {
    CallManager.unregisterCallingAccount()
    Cache.invalidate()

    return false
}
