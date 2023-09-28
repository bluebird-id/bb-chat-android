package id.bluebird.chat.sdk.app;

import static id.bluebird.chat.sdk.Const.FCM_TOKEN;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import id.bluebird.chat.sdk.Cache;

/**
 * Receives broadcasts to hang up or decline video/audio call.
 */
public class BReceiverRefreshToken extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String token = intent.getStringExtra(FCM_TOKEN);

        if (token != null && !token.equals("")) {
            Cache.getTinode().setDeviceToken(token);
        }
    }
}
