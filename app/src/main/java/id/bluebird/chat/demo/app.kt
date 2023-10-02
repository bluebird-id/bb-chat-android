package id.bluebird.chat.demo

import android.app.Application
import id.bluebird.chat.sdk.app.TindroidApp

class app : Application() {

    override fun onCreate() {
        super.onCreate()

        TindroidApp().onCreate(this)
        TindroidApp().setupWorkManager()
    }
}
