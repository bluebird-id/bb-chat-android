package id.bluebird.chat.sdk.feature.call;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import co.tinode.tinodesdk.AlreadySubscribedException;
import co.tinode.tinodesdk.ComTopic;
import co.tinode.tinodesdk.PromisedReply;
import co.tinode.tinodesdk.Tinode;
import co.tinode.tinodesdk.Topic;
import co.tinode.tinodesdk.model.ServerMessage;
import id.bluebird.chat.R;
import id.bluebird.chat.sdk.media.VxCard;
import id.bluebird.chat.sdk.Cache;
import id.bluebird.chat.sdk.CallManager;
import id.bluebird.chat.sdk.Const;
import id.bluebird.chat.sdk.feature.call.incoming.IncomingCallFragment;

public class CallActivity extends AppCompatActivity  {
    private static final String TAG = "CallActivity";

    static final String FRAGMENT_ACTIVE = "active_call";
    static final String FRAGMENT_INCOMING = "incoming_call";

    public static final String INTENT_ACTION_CALL_INCOMING = "tindroidx.intent.action.call.INCOMING";
    public static final String INTENT_ACTION_CALL_START = "tindroidx.intent.action.call.START";

    private boolean mTurnScreenOffWhenDone;

    private Tinode mTinode;

    private String mTopicName;
    private int mSeq;
    private ComTopic<VxCard> mTopic;
    private EventListener mLoginListener;

    private final BroadcastReceiver mFinishCallBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CallActivity.this.finishCall();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.cancel(CallManager.NOTIFICATION_TAG_INCOMING_CALL, 0);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mFinishCallBroadcastReceiver, new IntentFilter(Const.INTENT_ACTION_CALL_CLOSE));

        final Intent intent = getIntent();
        final String action = intent != null ? intent.getAction() : null;
        if (action == null) {
            Log.w(TAG, "No intent or no valid action, unable to proceed");
            finish();
            return;
        }

        // Using action once.
        intent.setAction(null);

        mTinode = Cache.getTinode();

        mTopicName = intent.getStringExtra(Const.INTENT_EXTRA_TOPIC);
        mSeq = intent.getIntExtra(Const.INTENT_EXTRA_SEQ, -1);
        // noinspection unchecked
        mTopic = (ComTopic<VxCard>) mTinode.getTopic(mTopicName);
        if (mTopic == null) {
            Log.e(TAG, "Invalid topic '" + mTopicName + "'");
            finish();
            return;
        }

        Cache.setSelectedTopicName(mTopicName);
        mLoginListener = new EventListener();
        mTinode.addListener(mLoginListener);

        Bundle args = new Bundle();
        String fragmentToShow;
        switch (action) {
            case INTENT_ACTION_CALL_INCOMING:
                // Incoming call started by the ser
                boolean accepted = intent.getBooleanExtra(Const.INTENT_EXTRA_CALL_ACCEPTED, false);
                fragmentToShow = accepted ? FRAGMENT_ACTIVE : FRAGMENT_INCOMING;
                args.putString(Const.INTENT_EXTRA_CALL_DIRECTION, "incoming");
                break;

            case INTENT_ACTION_CALL_START:
                // Call started by the current user.
                args.putString(Const.INTENT_EXTRA_CALL_DIRECTION, "outgoing");
                fragmentToShow = FRAGMENT_ACTIVE;
                break;

            default:
                Log.e(TAG, "Unknown call action '" + action + "'");
                finish();
                return;
        }
        setContentView(R.layout.activity_call);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // Turn screen on and unlock.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //KeyguardManager mgr = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            //mgr.requestDismissKeyguard(this, null);
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mTurnScreenOffWhenDone = !pm.isInteractive();
        // Try to reconnect and subscribe.
        topicAttach();
        showFragment(fragmentToShow, args);
    }

    @Override
    public void onDestroy() {
        if (mTinode != null) {
            mTinode.removeListener(mLoginListener);
        }
        Cache.endCallInProgress();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(mFinishCallBroadcastReceiver);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        if (mTurnScreenOffWhenDone && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false);
            setTurnScreenOn(false);
        }

        super.onDestroy();
    }

    public void acceptCall() {
        Bundle args = new Bundle();
        args.putString(Const.INTENT_EXTRA_CALL_DIRECTION, "incoming");
        showFragment(FRAGMENT_ACTIVE, args);
    }

    public void declineCall() {
        Cache.endCallInProgress();
        // Send message to server that the call is declined.
        if (mTopic != null) {
            mTopic.videoCallHangUp(mSeq);
        }
        finish();
    }

    public void finishCall() {
        finish();
    }

    void showFragment(String tag, Bundle args) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        FragmentManager fm = getSupportFragmentManager();

        Fragment fragment = fm.findFragmentByTag(tag);
        if (fragment == null) {
            switch (tag) {
                case FRAGMENT_INCOMING:
                    fragment = new IncomingCallFragment();
                    break;
                case FRAGMENT_ACTIVE:
                    fragment = new CallFragment();
                    break;
                default:
                    throw new IllegalArgumentException("Failed to create fragment: unknown tag " + tag);
            }
        } else if (args == null) {
            // Retain old arguments.
            args = fragment.getArguments();
        }

        args = args != null ? args : new Bundle();
        args.putString(Const.INTENT_EXTRA_TOPIC, mTopicName);
        args.putInt(Const.INTENT_EXTRA_SEQ, mSeq);

        if (fragment.getArguments() != null) {
            fragment.getArguments().putAll(args);
        } else {
            fragment.setArguments(args);
        }

        FragmentTransaction trx = fm.beginTransaction();
        if (!fragment.isAdded()) {
            trx = trx.replace(R.id.contentFragment, fragment, tag)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        } else if (!fragment.isVisible()) {
            trx = trx.show(fragment);
        }

        if (!trx.isEmpty()) {
            trx.commit();
        }
    }

    private void topicAttach() {
        if (mTopic.isAttached()) {
            mTopic.videoCallRinging(mSeq);
            return;
        }

        if (!mTinode.isAuthenticated()) {
            // If connection is not ready, wait for completion. This method will be called again
            // from the onLogin callback;
            Cache.getTinode().reconnectNow(true, false, false);
            return;
        }

        Topic.MetaGetBuilder builder = mTopic.getMetaGetBuilder()
                .withDesc()
                .withLaterData()
                .withDel();

        mTopic.subscribe(null, builder.build())
                .thenApply(new PromisedReply.SuccessListener<ServerMessage>() {
                    @Override
                    public PromisedReply<ServerMessage> onSuccess(ServerMessage result) {
                        mTopic.videoCallRinging(mSeq);
                        return null;
                    }
                })
                .thenCatch(new PromisedReply.FailureListener<ServerMessage>() {
                    @Override
                    public PromisedReply<ServerMessage> onFailure(Exception err) {
                        if (err instanceof AlreadySubscribedException) {
                            mTopic.videoCallRinging(mSeq);
                        } else {
                            Log.w(TAG, "Subscribe failed", err);
                            declineCall();
                        }
                        return null;
                    }
                });
    }

    private class EventListener implements Tinode.EventListener {
        @Override
        public void onLogin(int code, String txt) {
            if (code < ServerMessage.STATUS_MULTIPLE_CHOICES) {
                topicAttach();
            } else {
                declineCall();
            }
        }
    }
}
