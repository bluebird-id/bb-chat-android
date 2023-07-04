package id.bluebird.chat.sdk.feature;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import id.bluebird.chat.Const;
import id.bluebird.chat.db.BaseDb;
import id.bluebird.chat.sdk.feature.login.LoginActivity;
import id.bluebird.chat.sdk.feature.message.MessageActivity;

/**
 * Splash screen on startup
 */
public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // No need to check for live connection here.

        // Send user to appropriate screen:
        // 1. If we have an account and no credential validation is needed, send to ChatsActivity.
        // 2. If we don't have an account or credential validation is required send to LoginActivity.
        Intent launch = new Intent(this, BaseDb.getInstance().isReady() ?
                MessageActivity.class : LoginActivity.class);
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launch.putExtra(Const.INTENT_EXTRA_TOPIC, "usr7yG--GVH87o");
        startActivity(launch);
        finish();
    }
}