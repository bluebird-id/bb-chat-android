package id.bluebird.chat.sdk.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.util.Log;

import androidx.annotation.Nullable;

import id.bluebird.chat.sdk.CallManager;
import id.bluebird.chat.sdk.Const;

public class CallConnection extends Connection {
    private static final String TAG = "CallConnection";

    private final Context mContext;

    CallConnection(Context ctx) {
        super();
        mContext = ctx;
    }

    @Override
    public void onShowIncomingCallUi() {
        final String topicName = getAddress().getEncodedSchemeSpecificPart();
        CallManager.showIncomingCallUi(mContext, topicName, getExtras());
    }

    @Override
    public void onCallAudioStateChanged(@Nullable CallAudioState state) {
        Log.i(TAG, "onCallAudioStateChanged " + state);
    }

    @Override
    public void onAnswer() {
        Log.i(TAG, "onAnswer");
        Bundle args = getExtras();
        final String topicName = getAddress().getEncodedSchemeSpecificPart();
        Intent answer = CallManager.answerCallIntent(mContext, topicName, args.getInt(Const.INTENT_EXTRA_SEQ));
        mContext.startActivity(answer);
    }

    @Override
    public void onDisconnect() {
        // FIXME: this is never called by Android.
        Log.i(TAG, "onDisconnect");
        destroy();
    }

    @Override
    public void onHold() {
        Log.i(TAG, "onHold");
    }

    @Override
    public void onUnhold() {
        Log.i(TAG, "onUnhold");
    }

    @Override
    public void onReject() {
        Log.i(TAG, "onReject");
    }
}