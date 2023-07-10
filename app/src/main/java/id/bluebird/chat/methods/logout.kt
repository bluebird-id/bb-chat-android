package id.bluebird.chat.methods

import androidx.compose.runtime.MutableState
import id.bluebird.chat.sdk.Cache
import id.bluebird.chat.sdk.CallManager
import id.bluebird.chat.sdk.TindroidApp
import id.bluebird.chat.sdk.db.BaseDb

fun logout(
    isLogin: MutableState<Boolean>,
    db: BaseDb
) {
    CallManager.unregisterCallingAccount()
    TindroidApp.stopWatchingContacts()
    Cache.invalidate()

    isLogin.value = db.isReady
}
