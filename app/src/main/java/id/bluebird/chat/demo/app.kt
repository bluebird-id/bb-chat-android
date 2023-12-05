package id.bluebird.chat.demo

import android.app.Application
import id.bluebird.chat.sdk.app.BirdtalkApp

class app : Application() {

    override fun onCreate() {
        super.onCreate()

        BirdtalkApp().init(this,"driver-app","oSkIdsw8iUbQwTD33irfdHtAJ5IqFOPl")
        BirdtalkApp().setupWorkManager()
    }
}
