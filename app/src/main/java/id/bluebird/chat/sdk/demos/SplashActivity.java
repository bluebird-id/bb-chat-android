package id.bluebird.chat.sdk.demos;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import id.bluebird.chat.methods.message.MessageActivity;
import id.bluebird.chat.sdk.Const;
import id.bluebird.chat.sdk.db.BaseDb;
import id.bluebird.chat.sdk.demos.login.LoginActivity;

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
