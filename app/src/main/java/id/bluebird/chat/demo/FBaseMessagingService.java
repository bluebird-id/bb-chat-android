package id.bluebird.chat.demo;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import id.bluebird.chat.BBChat;
import id.bluebird.chat.sdk.services.BBFirebaseMessagingUtil;

public class FBaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull final String refreshedToken) {
        super.onNewToken(refreshedToken);
        BBChat.saveDeviceToken("6f436ccd-8041-4104-9688-8727882cf3da", refreshedToken, null, null);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        BBFirebaseMessagingUtil.onMessageReceived(remoteMessage);
    }
}
