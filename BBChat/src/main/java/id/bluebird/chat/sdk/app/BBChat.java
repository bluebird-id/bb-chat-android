package id.bluebird.chat.sdk.app;

import static id.bluebird.chat.sdk.Const.FCM_REFRESH_TOKEN;
import static id.bluebird.chat.sdk.Const.INTENT_ACTION_CALL_CLOSE;
import static id.bluebird.chat.sdk.account.Utils.PREFS_HOST_NAME;
import static id.bluebird.chat.sdk.account.Utils.TOKEN_EXPIRATION_TIME;
import static id.bluebird.chat.sdk.account.Utils.TOKEN_TYPE;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.WorkManager;

import com.android.installreferrer.api.InstallReferrerClient;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;

import co.tinode.tinodesdk.ServerResponseException;
import co.tinode.tinodesdk.Tinode;
import id.bluebird.chat.BuildConfig;
import id.bluebird.chat.R;
import id.bluebird.chat.sdk.BrandingConfig;
import id.bluebird.chat.sdk.Cache;
import id.bluebird.chat.sdk.Const;
import id.bluebird.chat.sdk.UiUtils;
import id.bluebird.chat.sdk.account.Utils;
import id.bluebird.chat.sdk.db.BaseDb;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * A class for providing global context for database access
 */
public class BBChat {
    private static final String TAG = "TindroidApp";
    private static Application application;
    private static String sAppVersion = null;
    private static int sAppBuild = 0;

    private static String hostname = "";
    private static Pair<String, Integer> chatServicesApi = new Pair<>("", 0);

    public void init(
            Application application,
            String hostname,
            Pair<String, Integer> chatServicesApi
    ) {
        BBChat.application = application;
        BBChat.hostname = hostname;
        BBChat.chatServicesApi = chatServicesApi;

        handeAppData();

        setupCrashlytics();

        createNotificationChannels();

        handleBroadcastManager();

        handleProcessLifecycleOwner();

        setupSharedPreferences();

        setupPicaso();

        listenConnectivity();
    }

    private void handeAppData() {
        try {
            PackageInfo pi = application.getPackageManager()
                    .getPackageInfo(application.getPackageName(), 0);
            sAppVersion = pi.versionName;
            if (TextUtils.isEmpty(sAppVersion)) {
                sAppVersion = BuildConfig.VERSION_NAME;
            }
            sAppBuild = pi.versionCode;
            if (sAppBuild <= 0) {
                sAppBuild = BuildConfig.VERSION_CODE;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Failed to retrieve app version", e);
        }
    }

    private void setupCrashlytics() {
//        Disable Crashlytics for debug builds.
//        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);
    }

    private void createNotificationChannels() {
        // Create the NotificationChannel on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel newMessage = new NotificationChannel(Const.NEWMSG_NOTIFICATION_CHAN_ID,
                    application.getString(R.string.new_message_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
            newMessage.setDescription(application.getString(R.string.new_message_channel_description));
            newMessage.enableLights(true);
            newMessage.setLightColor(Color.WHITE);

            NotificationChannel videoCall = new NotificationChannel(Const.CALL_NOTIFICATION_CHAN_ID,
                    application.getString(R.string.video_call_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            videoCall.setDescription(application.getString(R.string.video_call_channel_description));
            videoCall.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .build());
            videoCall.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            videoCall.enableVibration(true);
            videoCall.enableLights(true);
            videoCall.setLightColor(Color.RED);

            NotificationManager nm = application.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(newMessage);
                nm.createNotificationChannel(videoCall);
            }
        }
    }

    private void handleBroadcastManager() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(application);
        lbm.registerReceiver(new BReceiverRefreshToken(), new IntentFilter(FCM_REFRESH_TOKEN));
        lbm.registerReceiver(new BReceiverHangUp(), new IntentFilter(INTENT_ACTION_CALL_CLOSE));
    }

    private void handleProcessLifecycleOwner() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                // Check if the app was installed from an URL with attributed installation source.
                // If yes, get the config from hosts.tinode.co.
                if (UiUtils.isAppFirstRun(application)) {
                    Executors.newSingleThreadExecutor().execute(() ->
                            BrandingConfig.getInstallReferrerFromClient(application,
                                    InstallReferrerClient.newBuilder(application).build()));
                }

                // Check if the app has an account already. If so, initialize the shared connection with the server.
                // Initialization may fail if device is not connected to the network.
                String uid = BaseDb.getInstance().getUid();
                if (!TextUtils.isEmpty(uid)) {
                    new LoginWithSavedAccount().execute(uid);
                }
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                // Disconnect now, so the connection does not wait for the timeout.
                Tinode tinode = Cache.getTinode();
                if (tinode != null) {
                    tinode.maybeDisconnect(false);
                }
            }
        });
    }

    // Read saved account credentials and try to connect to server using them.
    // Suppressed lint warning because TindroidApp won't leak: it must exist for the entire lifetime of the app.
    @SuppressLint("StaticFieldLeak")
    private class LoginWithSavedAccount extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... uidWrapper) {
            Tinode tinode = Cache.getTinode();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getAppContext());
            String token = prefs.getString(TOKEN_TYPE, "");

            if (!TextUtils.isEmpty(token)) {
                // Account found, establish connection to the server and use save account credentials for login.
                Date expires = null;
                try {
                    String strExp = prefs.getString(TOKEN_EXPIRATION_TIME, "");
                    // FIXME: remove this check when all clients are updated; Apr 8, 2020.
                    if (!TextUtils.isEmpty(strExp)) {
                        expires = new Date(Long.parseLong(strExp));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failure to login with saved account", e);
                }

                if (!TextUtils.isEmpty(token) && expires != null && expires.after(new Date())) {
                    // Connecting with synchronous calls because this is not the UI thread.
                    tinode.setAutoLoginToken(token);
                    // Connect and login.
                    try {
                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(application);
                        // Sync call throws on error.
                        tinode.connect(pref.getString(PREFS_HOST_NAME, getDefaultHostName()),
                                pref.getBoolean(Utils.PREFS_USE_TLS, getDefaultTLS()),
                                false).getResult();
                        if (!tinode.isAuthenticated()) {
                            // The connection may already exist but not yet authenticated.
                            tinode.loginToken(token).getResult();
                        }
                        Cache.attachMeTopic(null);
                        // Logged in successfully. Save refreshed token for future use.
                        prefs.edit().putString(TOKEN_TYPE, tinode.getAuthToken());
                        prefs.edit().putString(TOKEN_EXPIRATION_TIME, String.valueOf(tinode.getAuthTokenExpiration().getTime()));
                    } catch (IOException ex) {
                        Log.d(TAG, "Network failure during login", ex);
                        // Do not invalidate token on network failure.
                    } catch (ServerResponseException ex) {
                        Log.w(TAG, "Server rejected login sequence", ex);
                        int code = ex.getCode();
                        // 401: Token expired or invalid login.
                        // 404: 'me' topic is not found (user deleted, but token is still valid).
                        if (code == 401 || code == 404) {
                            // Another try-catch because some users revoke needed permission after granting it.
                            try {
                                // Login failed due to invalid (expired) token or missing/disabled account.
                                prefs.edit().putString(TOKEN_EXPIRATION_TIME, "");
                            } catch (SecurityException ex2) {
                                Log.e(TAG, "Unable to access android account", ex2);
                            }
                            // Force new login.
                            UiUtils.doLogout(application);
                        }
                        // 409 Already authenticated should not be possible here.
                    } catch (Exception ex) {
                        Log.e(TAG, "Other failure during login", ex);
                    }
                } else {
                    Log.i(TAG, "No token or expired token. Forcing re-login");
                    try {
                        prefs.edit().putString(TOKEN_EXPIRATION_TIME, "");
                    } catch (SecurityException ex) {
                        Log.e(TAG, "Unable to access android account", ex);
                    }
                    // Force new login.
                    UiUtils.doLogout(application);
                }
            } else {
                Log.i(TAG, "Account not found or no permission to access accounts");
                // Force new login in case account existed before but was deleted.
                UiUtils.doLogout(application);
            }
            return null;
        }
    }

    private void setupSharedPreferences() {
        // Check if preferences already exist. If not, create them.
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(application);
        if (TextUtils.isEmpty(pref.getString(PREFS_HOST_NAME, null))) {
            // No preferences found. Save default values.
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(PREFS_HOST_NAME, getDefaultHostName());
            editor.putBoolean(Utils.PREFS_USE_TLS, getDefaultTLS());
            editor.apply();
        }
    }

    public static String getDefaultHostName() {
        return hostname;
    }

    public static Pair<String, Integer> getChatServicesApi() {
        return chatServicesApi;
    }

    public static boolean getDefaultTLS() {
        return !isEmulator();
    }

    // Detect if the code is running in an emulator.
    // Used mostly for convenience to use correct server address i.e. 10.0.2.2:6060 vs sandbox.tinode.co and
    // to enable/disable Crashlytics. It's OK if it's imprecise.
    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("sdk_gphone_x86")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT)
                || Build.PRODUCT.startsWith("sdk")
                || Build.PRODUCT.startsWith("vbox");
    }

    public void setupWorkManager() {
        // Clear completed/failed upload tasks.
        WorkManager.getInstance(application).pruneWork();
    }

    private void setupPicaso() {
        Tinode tinode = Cache.getTinode();
        int PICASSO_CACHE_SIZE = 1024 * 1024 * 256;

        // Setting up Picasso with auth headers.
        OkHttpClient client = new OkHttpClient.Builder()
                .cache(new okhttp3.Cache(createDefaultCacheDir(), PICASSO_CACHE_SIZE))
                .addInterceptor(chain -> {
                    Request picassoReq = chain.request();
                    Map<String, String> headers;
                    if (tinode.isTrustedURL(picassoReq.url().url())) {
                        headers = tinode.getRequestHeaders();
                        Request.Builder builder = picassoReq.newBuilder();
                        for (Map.Entry<String, String> el : headers.entrySet()) {
                            builder = builder.addHeader(el.getKey(), el.getValue());
                        }
                        return chain.proceed(builder.build());
                    } else {
                        return chain.proceed(picassoReq);
                    }
                })
                .build();

        Picasso.setSingletonInstance(new Picasso.Builder(application)
                .requestTransformer(request -> {
                    // Rewrite relative URIs to absolute.
                    if (request.uri != null && Tinode.isUrlRelative(request.uri.toString())) {
                        URL url = tinode.toAbsoluteURL(request.uri.toString());
                        if (url != null) {
                            return request.buildUpon().setUri(Uri.parse(url.toString())).build();
                        }
                    }
                    return request;
                })
                .downloader(new OkHttp3Downloader(client))
                .build());
    }

    private static File createDefaultCacheDir() {
        File cache = new File(application.getCacheDir(), "picasso-cache");
        if (!cache.exists()) {
            // noinspection ResultOfMethodCallIgnored
            cache.mkdirs();
        }
        return cache;
    }

    private void listenConnectivity() {
        // Listen to connectivity changes.
        ConnectivityManager cm = (ConnectivityManager) application.getSystemService(
                application.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkRequest req = new NetworkRequest.
                    Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();

            cm.registerNetworkCallback(req, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    Tinode tinode = Cache.getTinode();
                    BaseDb baseDb = BaseDb.getInstance();

                    if (!TextUtils.isEmpty(baseDb.getUid())) {
                        // Connect right away if UID is available.
                        tinode.reconnectNow(true, false, false);
                    }
                }
            });
        }
    }

    // The Tinode cache is linked from here so it's never garbage collected.
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private static Cache sCache;

    public static void retainCache(Cache cache) {
        sCache = cache;
    }

    public static Context getAppContext() {
        return application;
    }

    public static String getAppVersion() {
        return sAppVersion;
    }
}
