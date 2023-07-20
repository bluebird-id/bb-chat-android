package id.bluebird.chat.sdk.demos.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import id.bluebird.chat.R;
import id.bluebird.chat.methods.message.MessageActivity;
import id.bluebird.chat.sdk.Const;
import id.bluebird.chat.sdk.UiUtils;
import id.bluebird.chat.sdk.db.BaseDb;

/**
 * LoginActivity is a FrameLayout which switches between fragments:
 * - LoginFragment
 * - SignUpFragment
 * - LoginSettingsFragment
 * - PasswordResetFragment
 * - CredentialsFragment
 * <p>
 * 1. If connection to the server is already established and authenticated, launch ContactsActivity
 * 2. If no connection to the server, get the last used account:
 * 3.1 Connect to server
 * 3.1.1 If connection is successful, authenticate with the token received from the account
 * 3.1.1.1 If authentication is successful go to 1.
 * 3.1.1.2 If not, go to 4.
 * 3.1.2 If connection is not successful
 * 3.1.2 Show offline indicator
 * 3.1.3 Access locally stored account.
 * 3.1.3.1 If locally stored account is found, launch ContactsActivity
 * 3.1.3.2 If not found, go to 4.
 * 4. If account not found, show login form
 */

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    public static final String EXTRA_CONFIRM_CREDENTIALS = "confirmCredentials";
    public static final String EXTRA_ADDING_ACCOUNT = "addNewAccount";
    static final String FRAGMENT_LOGIN = "login";
    static final String FRAGMENT_SIGNUP = "signup";
    static final String FRAGMENT_SETTINGS = "settings";
    static final String FRAGMENT_RESET = "reset";
    static final String FRAGMENT_CREDENTIALS = "cred";
    static final String FRAGMENT_AVATAR_PREVIEW = "avatar_preview";

    static final String FRAGMENT_BRANDING = "branding";
    static final String PREFS_LAST_LOGIN = "pref_lastLogin";

    static {
        // Otherwise crash on API 21.
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        PreferenceManager.setDefaultValues(this, R.xml.login_preferences, false);

        BaseDb db = BaseDb.getInstance();
        if (db.isReady()) {
            // We already have a configured account. All good. Launch ContactsActivity and stop.
            Intent intent = new Intent(this, MessageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(Const.INTENT_EXTRA_TOPIC, "usr7yG--GVH87o");
            startActivity(intent);
            finish();
            return;
        }

        // Check if we need full authentication or just credentials.
        showFragment(db.isCredValidationRequired() ? FRAGMENT_CREDENTIALS : FRAGMENT_LOGIN,
                null, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        UiUtils.setupToolbar(this, null, null, false, null, false);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    void reportError(final Exception err, final Button button, final int attachTo, final int errId) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        String message = getText(errId).toString();
        String errMessage = err != null ? err.getMessage() : "";

        if (err != null) {
            Throwable cause = err;
            while ((cause = cause.getCause()) != null) {
                errMessage = cause.getMessage();
            }
        }
        final String finalMessage = message +
                (!TextUtils.isEmpty(errMessage) ? (": " + errMessage + "") : "");
        Log.i(TAG, finalMessage, err);

        runOnUiThread(() -> {
            if (button != null) {
                button.setEnabled(true);
            }
            EditText field = attachTo != 0 ? (EditText) findViewById(attachTo) : null;
            if (field != null) {
                field.setError(finalMessage);
            } else {
                Toast.makeText(LoginActivity.this, finalMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    void showFragment(String tag, Bundle args) {
        showFragment(tag, args, true);
    }

    private void showFragment(String tag, Bundle args, Boolean addToBackstack) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(tag);
        if (fragment == null) {
            if (FRAGMENT_LOGIN.equals(tag)) {
                fragment = new LoginFragment();
            } else {
                throw new IllegalArgumentException();
            }
        }

        if (fragment.getArguments() != null) {
            fragment.getArguments().putAll(args);
        } else {
            fragment.setArguments(args);
        }

        FragmentTransaction tx = fm.beginTransaction()
                .replace(R.id.contentFragment, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        if (addToBackstack) {
            tx = tx.addToBackStack(null);
        }
        tx.commitAllowingStateLoss();
    }
}
