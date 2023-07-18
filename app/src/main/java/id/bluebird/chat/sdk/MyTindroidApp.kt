package id.bluebird.chat.sdk

import android.accounts.Account
import android.content.Context
import androidx.annotation.NonNull
import androidx.lifecycle.LifecycleOwner

class MyTindroidApp {

    companion object {

        @Synchronized
        fun startWatchingContacts(context: Context, acc: Account) {
            TindroidApp.startWatchingContacts(context, acc)
        }
    }
}