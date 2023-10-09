package id.bluebird.chat.sdk.account;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.ContactsContract.Data;

import androidx.preference.PreferenceManager;

/**
 * Constants and misc utils
 */
public class Utils {
    // Account management constants
    public static final String TOKEN_TYPE = "co.tinode.token";
    public static final String TOKEN_EXPIRATION_TIME = "co.tinode.token_expires";
    // Constants for accessing shared preferences
    public static final String PREFS_HOST_NAME = "pref_hostName";
    public static final String PREFS_USE_TLS = "pref_useTLS";
    /**
     * MIME-type used when storing a profile {@link Data} entry.
     */
    public static final String MIME_TINODE_PROFILE =
            "vnd.android.cursor.item/vnd.co.tinode.im";
    public static final String DATA_PID = Data.DATA1;

    /**
     * Obtain authentication token for the currently active account.
     *
     * @param context application context.
     * @return token or null.
     */
    public static String getLoginToken(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(TOKEN_TYPE, "");
    }
}
