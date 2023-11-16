package id.bluebird.chat.demo

import android.app.Application
import androidx.core.util.Pair
import id.bluebird.chat.sdk.app.TindroidApp

class app : Application() {

    override fun onCreate() {
        super.onCreate()

        TindroidApp().onCreate(this, "dev-tinode.bluebird.id", Pair("34.124.216.166", 6969))
        TindroidApp().setupWorkManager()
    }
}
