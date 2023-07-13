package id.bluebird.chat.sdk

import android.accounts.Account
import android.content.Context

class MyTindroidApp {

    companion object {

        @Synchronized
        fun startWatchingContacts(context: Context, acc: Account) {
            TindroidApp.startWatchingContacts(context, acc)
        }
    }
}