package id.bluebird.chat.demo

import android.app.Application
import androidx.core.util.Pair
import id.bluebird.chat.sdk.app.BBChat

class app : Application() {

    override fun onCreate() {
        super.onCreate()

        BBChat().init(
            /* application = */ this,
            /* hostname = */ "dev-tinode.bluebird.id",
            /* chatServicesApi = */ Pair("34.124.216.166", 6969),
        )
        BBChat().setupWorkManager()
    }
}
