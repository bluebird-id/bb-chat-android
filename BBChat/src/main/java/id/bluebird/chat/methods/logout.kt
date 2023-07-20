package id.bluebird.chat.methods

import id.bluebird.chat.sdk.Cache
import id.bluebird.chat.sdk.CallManager
import id.bluebird.chat.sdk.TindroidApp

fun logout(): Boolean {
    CallManager.unregisterCallingAccount()
    TindroidApp.stopWatchingContacts()
    Cache.invalidate()

    return false
}
