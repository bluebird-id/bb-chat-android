package id.bluebird.chat.sdk.services;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;

import androidx.annotation.Nullable;

import id.bluebird.chat.sdk.Cache;
import id.bluebird.chat.sdk.CallManager;
import id.bluebird.chat.sdk.Const;

public class CallConnectionService extends ConnectionService {
    private static final String TAG = "CallConnectionService";

    @Override
    public Connection onCreateOutgoingConnection(@Nullable PhoneAccountHandle connectionManagerPhoneAccount,
                                                 @Nullable ConnectionRequest request) {
        Log.i(TAG, "onCreateOutgoingConnection");

        CallConnection conn = new CallConnection(getApplicationContext());
        conn.setInitializing();
        if (request != null) {
            conn.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
            conn.setVideoState(request.getVideoState());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            conn.setConnectionProperties(Connection.PROPERTY_SELF_MANAGED);
        }
        conn.setConnectionCapabilities(Connection.CAPABILITY_MUTE |
                Connection.CAPABILITY_CAN_SEND_RESPONSE_VIA_CONNECTION);
        conn.setAudioModeIsVoip(true);
        conn.setRinging();

        String topicName = conn.getAddress().getSchemeSpecificPart();

        CallManager.showOutgoingCallUi(this, topicName, conn);

        return conn;
    }

    @Override
    public Connection onCreateIncomingConnection(@Nullable PhoneAccountHandle connectionManagerPhoneAccount,
                                                 @Nullable ConnectionRequest request) {
        if (request == null) {
            Log.w(TAG, "Dropped incoming call with null ConnectionRequest");
            return null;
        }

        CallConnection conn = new CallConnection(getApplicationContext());
        conn.setInitializing();
        final Uri callerUri = request.getAddress();
        conn.setAddress(callerUri, TelecomManager.PRESENTATION_ALLOWED);

        Bundle callParams = request.getExtras();
        Bundle extras = callParams.getBundle(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS);
        if (extras == null) {
            Log.w(TAG, "Dropped incoming due to null extras");
            return null;
        }

        int seq = extras.getInt(Const.INTENT_EXTRA_SEQ);
        conn.setExtras(extras);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            conn.setConnectionProperties(Connection.PROPERTY_SELF_MANAGED);
        }

        Cache.prepareNewCall(callerUri.getSchemeSpecificPart(), seq, conn);

        conn.setConnectionCapabilities(Connection.CAPABILITY_MUTE);
        conn.setAudioModeIsVoip(true);
        conn.setActive();

        return conn;
    }

    @Override
    public void onCreateIncomingConnectionFailed(@Nullable PhoneAccountHandle connectionManagerPhoneAccount,
                                                 @Nullable ConnectionRequest request) {
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request);
        Log.i(TAG, "Create incoming call failed");
    }

    @Override
    public void onCreateOutgoingConnectionFailed(@Nullable PhoneAccountHandle connectionManagerPhoneAccount,
                                                 @Nullable ConnectionRequest request) {
        super.onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount, request);
        Log.i(TAG, "Create outgoing call failed");
    }
}
