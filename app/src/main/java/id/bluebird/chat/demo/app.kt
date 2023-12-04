package id.bluebird.chat.demo

import android.app.Application
import id.bluebird.chat.sdk.app.BirdtalkApp

class app : Application() {

    override fun onCreate() {
        super.onCreate()

        BirdtalkApp().init(/* application = */ this)
        BirdtalkApp().setupWorkManager()
    }
}
