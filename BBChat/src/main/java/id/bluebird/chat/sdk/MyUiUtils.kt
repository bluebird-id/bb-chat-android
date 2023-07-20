package id.bluebird.chat.sdk

import android.accounts.Account

class MyUiUtils {

    companion object {

        @Synchronized
        fun requestImmediateContactsSync(acc: Account?) {
            UiUtils.requestImmediateContactsSync(acc)
        }
    }
}