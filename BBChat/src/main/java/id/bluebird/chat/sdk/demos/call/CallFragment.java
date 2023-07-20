package id.bluebird.chat.sdk.demos.call;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import co.tinode.tinodesdk.ComTopic;
import co.tinode.tinodesdk.PromisedReply;
import co.tinode.tinodesdk.Tinode;
import co.tinode.tinodesdk.Topic;
import co.tinode.tinodesdk.model.Drafty;
import co.tinode.tinodesdk.model.MsgServerInfo;
import co.tinode.tinodesdk.model.PrivateType;
import co.tinode.tinodesdk.model.ServerMessage;
import id.bluebird.chat.R;
import id.bluebird.chat.sdk.Cache;
import id.bluebird.chat.sdk.Const;
import id.bluebird.chat.sdk.UiUtils;
import id.bluebird.chat.sdk.media.VxCard;

/**
 * Video call UI: local & remote video views.
 */
public class CallFragment extends Fragment {
    private static final String TAG = "CallFragment";

    // Video mute/unmute events.
    private static final String VIDEO_MUTED_EVENT = "video:muted";
    private static final String VIDEO_UNMUTED_EVENT = "video:unmuted";

    // Camera constants.
    // TODO: hardcoded for now. Consider querying camera for supported values.
    private static final int CAMERA_RESOLUTION_WIDTH = 1024;
    private static final int CAMERA_RESOLUTION_HEIGHT = 720;
    private static final int CAMERA_FPS = 30;

    public enum CallDirection {
        OUTGOING,
        INCOMING,
    }

    private PeerConnectionFactory mPeerConnectionFactory;
    private MediaConstraints mSdpConstraints;
    private AudioSource mAudioSource;
    private AudioTrack mLocalAudioTrack;
    private PeerConnection mLocalPeer;
    private DataChannel mDataChannel;
    private List<PeerConnection.IceServer> mIceServers;

    private CallDirection mCallDirection;
    // If true, the client has received a remote SDP from the peer and has sent a local SDP to the peer.
    private boolean mCallInitialSetupComplete;
    // Stores remote ice candidates until initial call setup is complete.
    private List<IceCandidate> mRemoteIceCandidatesCache;

    // Media state
    private boolean mAudioOff = false;

    // For playing ringing sounds.
    MediaPlayer mMediaPlayer = null;

    // Control buttons: speakerphone, mic, camera.
    private FloatingActionButton mToggleSpeakerphoneBtn;
    private FloatingActionButton mToggleMicBtn;

    private ConstraintLayout mLayout;
    private TextView mPeerName;
    private ImageView mPeerAvatar;

    private ComTopic<VxCard> mTopic;
    private int mCallSeqID = 0;
    private InfoListener mTinodeListener;
    private boolean mCallStarted = false;

    // Check if we have camera and mic permissions.
    private final ActivityResultLauncher<String[]> mMediaPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                for (Map.Entry<String, Boolean> e : result.entrySet()) {
                    if (!e.getValue()) {
                        Log.d(TAG, "The user has disallowed " + e);
                        handleCallClose();
                        return;
                    }
                }
                // All permissions granted.
                startMediaAndSignal();
            });

    public CallFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_call, container, false);

        mToggleSpeakerphoneBtn = v.findViewById(R.id.toggleSpeakerphoneBtn);
        mToggleMicBtn = v.findViewById(R.id.toggleMicBtn);

        mLayout = v.findViewById(R.id.callMainLayout);

        // Button click handlers: speakerphone on/off, mute/unmute, video/audio-only, hang up.
        mToggleSpeakerphoneBtn.setOnClickListener(v0 ->
                toggleSpeakerphone((FloatingActionButton) v0));
        v.findViewById(R.id.hangupBtn).setOnClickListener(v1 -> handleCallClose());
        mToggleMicBtn.setOnClickListener(v3 ->
                toggleMedia((FloatingActionButton) v3,
                        R.drawable.ic_mic, R.drawable.ic_mic_off));
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstance) {
        final Activity activity = requireActivity();
        final Bundle args = getArguments();
        if (args == null) {
            Log.w(TAG, "Call fragment created with no arguments");
            // Reject the call.
            handleCallClose();
            return;
        }

        Tinode tinode = Cache.getTinode();
        String name = args.getString(Const.INTENT_EXTRA_TOPIC);
        // noinspection unchecked
        mTopic = (ComTopic<VxCard>) tinode.getTopic(name);

        String callStateStr = args.getString(Const.INTENT_EXTRA_CALL_DIRECTION);
        mCallDirection = "incoming".equals(callStateStr) ? CallDirection.INCOMING : CallDirection.OUTGOING;
        if (mCallDirection == CallDirection.INCOMING) {
            mCallSeqID = args.getInt(Const.INTENT_EXTRA_SEQ);
        }

        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(false);
        mToggleSpeakerphoneBtn.setImageResource(R.drawable.ic_volume_off);

        if (!mTopic.isAttached()) {
            mTopic.setListener(new Topic.Listener<VxCard, PrivateType, VxCard, PrivateType>() {
                @Override
                public void onSubscribe(int code, String text) {
                    handleCallStart();
                }
            });
        }

        mTinodeListener = new InfoListener();
        tinode.addListener(mTinodeListener);

        VxCard pub = mTopic.getPub();
        mPeerAvatar = view.findViewById(R.id.imageAvatar);
        UiUtils.setAvatar(mPeerAvatar, pub, name, false);

        String peerName = pub != null ? pub.fn : null;
        if (TextUtils.isEmpty(peerName)) {
            peerName = getResources().getString(R.string.unknown);
        }
        mPeerName = view.findViewById(R.id.peerName);
        mPeerName.setText(peerName);

        mRemoteIceCandidatesCache = new ArrayList<>();

        // Check permissions.
        LinkedList<String> missing = UiUtils.getMissingPermissions(activity,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO});
        if (!missing.isEmpty()) {
            mMediaPermissionLauncher.launch(missing.toArray(new String[]{}));
            return;
        }

        // Got all necessary permissions.
        startMediaAndSignal();
    }

    @Override
    public void onDestroyView() {
        stopMediaAndSignal();
        Cache.getTinode().removeListener(mTinodeListener);
        mTopic.setListener(null);

        Context ctx = getContext();
        if (ctx != null) {
            AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setMode(AudioManager.MODE_NORMAL);
                audioManager.setMicrophoneMute(false);
                audioManager.setSpeakerphoneOn(false);
            }
        }

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        stopSoundEffect();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        stopSoundEffect();
        super.onPause();
    }

    private void enableControls() {
        requireActivity().runOnUiThread(() -> {
            mToggleSpeakerphoneBtn.setEnabled(true);
            mToggleMicBtn.setEnabled(true);
        });
    }

    private static VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                } else {
                    Log.d(TAG, "Failed to create FF camera " + deviceName);
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    // Mute/unmute media.
    private void toggleMedia(FloatingActionButton b, @DrawableRes int enabledIcon, int disabledIcon) {
        boolean disabled;

        disabled = !mAudioOff;
        mAudioOff = disabled;

        b.setImageResource(disabled ? disabledIcon : enabledIcon);

        mLocalAudioTrack.setEnabled(!disabled);

        // Need to disable microphone too, otherwise webrtc LocalPeer produces echo.
        AudioManager audioManager = (AudioManager) b.getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setMicrophoneMute(disabled);
        }

        if (mLocalPeer == null) {
            return;
        }

        for (RtpSender transceiver : mLocalPeer.getSenders()) {
            MediaStreamTrack track = transceiver.track();
            if (track instanceof AudioTrack) {
                track.setEnabled(!disabled);
            }
        }
    }

    private void toggleSpeakerphone(FloatingActionButton b) {
        AudioManager audioManager = (AudioManager) b.getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            boolean enabled = audioManager.isSpeakerphoneOn();
            audioManager.setSpeakerphoneOn(!enabled);
            b.setImageResource(enabled ? R.drawable.ic_volume_off : R.drawable.ic_volume_up);
        }
    }

    // Initializes media (camera and audio) and notifies the peer (sends "invite" for outgoing,
    // and "accept" for incoming call).
    private void startMediaAndSignal() {
        final Activity activity = requireActivity();
        if (activity.isFinishing() || activity.isDestroyed()) {
            // We are done. Just quit.
            return;
        }

        if (!initIceServers()) {
            Toast.makeText(activity, R.string.video_calls_unavailable, Toast.LENGTH_LONG).show();
            handleCallClose();
        }

        // Initialize PeerConnectionFactory globals.
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(activity)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        // Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        PeerConnectionFactory.Builder pcfBuilder = PeerConnectionFactory.builder()
                .setOptions(options);

        mPeerConnectionFactory = pcfBuilder.createPeerConnectionFactory();

        // Create MediaConstraints - Will be useful for specifying video and audio constraints.
        MediaConstraints audioConstraints = new MediaConstraints();

        // Create an AudioSource instance
        mAudioSource = mPeerConnectionFactory.createAudioSource(audioConstraints);
        mLocalAudioTrack = mPeerConnectionFactory.createAudioTrack("101", mAudioSource);
        if (mAudioOff) {
            mLocalAudioTrack.setEnabled(false);
        }

        handleCallStart();
    }

    // Stops media and concludes the call (sends "hang-up" to the peer).
    private void stopMediaAndSignal() {
        // Clean up.
        if (mLocalPeer != null) {
            mLocalPeer.close();
            mLocalPeer = null;
        }

        if (mAudioSource != null) {
            mAudioSource.dispose();
            mAudioSource = null;
        }

        handleCallClose();
    }

    private boolean initIceServers() {
        mIceServers = new ArrayList<>();
        try {
            //noinspection unchecked
            List<Map<String, Object>> iceServersConfig =
                    (List<Map<String, Object>>) Cache.getTinode().getServerParam("iceServers");
            if (iceServersConfig == null) {
                return false;
            }

            for (Map<String, Object> server : iceServersConfig) {
                //noinspection unchecked
                List<String> urls = (List<String>) server.get("urls");
                if (urls == null || urls.isEmpty()) {
                    Log.w(TAG, "Invalid ICE server config: no URLs");
                    continue;
                }
                PeerConnection.IceServer.Builder builder = PeerConnection.IceServer.builder(urls);
                String username = (String) server.get("username");
                if (username != null) {
                    builder.setUsername(username);
                }
                String credential = (String) server.get("credential");
                if (credential != null) {
                    builder.setPassword(credential);
                }
                mIceServers.add(builder.createIceServer());
            }
        } catch (ClassCastException | NullPointerException ex) {
            Log.w(TAG, "Unexpected format of server-provided ICE config", ex);
            return false;
        }
        return !mIceServers.isEmpty();
    }

    private void addRemoteIceCandidateToCache(IceCandidate candidate) {
        mRemoteIceCandidatesCache.add(candidate);
    }

    private void drainRemoteIceCandidatesCache() {
        Log.d(TAG, "Draining remote ICE candidate cache: " + mRemoteIceCandidatesCache.size() + " elements.");
        for (IceCandidate candidate : mRemoteIceCandidatesCache) {
            mLocalPeer.addIceCandidate(candidate);
        }
        mRemoteIceCandidatesCache.clear();
    }

    // Peers have exchanged their local and remote SDPs.
    private void initialSetupComplete() {
        mCallInitialSetupComplete = true;
        drainRemoteIceCandidatesCache();
        rearrangePeerViews(requireActivity(), false);
        enableControls();
    }

    // Sends a hang-up notification to the peer and closes the fragment.
    private void handleCallClose() {
        stopSoundEffect();

        // Close fragment.
        if (mCallSeqID > 0) {
            mTopic.videoCallHangUp(mCallSeqID);
        }

        mCallSeqID = -1;
        final CallActivity activity = (CallActivity) getActivity();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.finishCall();
        }
        Cache.endCallInProgress();
    }

    // Call initiation.
    private void handleCallStart() {
        if (!mTopic.isAttached() || mCallStarted) {
            // Already started or not attached. wait to attach.
            return;
        }
        Activity activity = requireActivity();
        mCallStarted = true;
        switch (mCallDirection) {
            case OUTGOING:
                // Send out a call invitation to the peer.
                Map<String, Object> head = new HashMap<>();
                head.put("webrtc", "started");
                // Is audio-only?
                head.put("aonly", true);
                mTopic.publish(Drafty.videoCall(), head).thenApply(
                        new PromisedReply.SuccessListener<ServerMessage>() {
                            @Override
                            public PromisedReply<ServerMessage> onSuccess(ServerMessage result) {
                                if (result.ctrl != null && result.ctrl.code < 300) {
                                    int seq = result.ctrl.getIntParam("seq", -1);
                                    if (seq > 0) {
                                        // All good.
                                        mCallSeqID = seq;
                                        Cache.setCallActive(mTopic.getName(), seq);
                                        return null;
                                    }
                                }
                                handleCallClose();
                                return null;
                            }
                        }, new FailureHandler(getActivity()));
                rearrangePeerViews(activity, false);
                break;
            case INCOMING:
                // The callee (we) has accepted the call. Notify the caller.
                rearrangePeerViews(activity, false);
                mTopic.videoCallAccept(mCallSeqID);
                Cache.setCallConnected();
                break;
            default:
                break;
        }
    }

    // Sends a SDP offer to the peer.
    private void handleSendOffer(SessionDescription sd) {
        mTopic.videoCallOffer(mCallSeqID, new SDPAux(sd.type.canonicalForm(), sd.description));
    }

    // Sends a SDP answer to the peer.
    private void handleSendAnswer(SessionDescription sd) {
        mTopic.videoCallAnswer(mCallSeqID, new SDPAux(sd.type.canonicalForm(), sd.description));
    }

    private void sendToPeer(String msg) {
        if (mDataChannel != null) {
            mDataChannel.send(new DataChannel.Buffer(
                    ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)), false));
        } else {
            Log.w(TAG, "Data channel is null. Peer will not receive the message: '" + msg + "'");
        }
    }

    // Creates and initializes a peer connection.
    private void createPeerConnection(boolean withDataChannel) {
        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(mIceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;

        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.PLAN_B;
        mLocalPeer = mPeerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                // Send ICE candidate to the peer.
                handleIceCandidateEvent(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                // Received remote stream.
                Activity activity = requireActivity();
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
            }

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(TAG, "onSignalingChange() called with: signalingState = [" + signalingState + "]");
                if (signalingState == PeerConnection.SignalingState.CLOSED) {
                    handleCallClose();
                }
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "onIceConnectionChange() called with: iceConnectionState = [" + iceConnectionState + "]");
                switch (iceConnectionState) {
                    case CLOSED:
                    case FAILED:
                        handleCallClose();
                        break;
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d(TAG, "onIceConnectionReceivingChange() called with: b = [" + b + "]");
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(TAG, "onIceGatheringChange() called with: iceGatheringState = [" + iceGatheringState + "]");
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d(TAG, "onIceCandidatesRemoved() called with: iceCandidates = [" +
                        Arrays.toString(iceCandidates) + "]");
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(TAG, "onRemoveStream() called with: mediaStream = [" + mediaStream + "]");
            }

            @Override
            public void onDataChannel(DataChannel channel) {
                Log.d(TAG, "onDataChannel(): state: " + channel.state());
                mDataChannel = channel;
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded() called");

                if (CallFragment.this.mCallDirection == CallDirection.INCOMING &&
                        !CallFragment.this.mCallInitialSetupComplete) {
                    // Do not send an offer yet as
                    // - We are still in initial setup phase.
                    // - The caller is supposed to send us an offer.
                    return;
                }
                if (mLocalPeer.getSenders().isEmpty()) {
                    // This is a recvonly connection for now. Wait until it turns sendrecv.
                    Log.i(TAG, "PeerConnection is recvonly. Waiting for sendrecv.");
                    return;
                }
                mSdpConstraints = new MediaConstraints();
                mSdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
                mLocalPeer.createOffer(new CustomSdpObserver("localCreateOffer") {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        super.onCreateSuccess(sessionDescription);
                        Log.d("onCreateSuccess", "setting local desc - setLocalDescription");
                        mLocalPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"),
                                sessionDescription);
                        handleSendOffer(sessionDescription);
                    }
                }, mSdpConstraints);
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                Log.d(TAG, "onAddTrack() called with: rtpReceiver = [" + rtpReceiver +
                        "], mediaStreams = [" + Arrays.toString(mediaStreams) + "]");
            }
        });

        if (withDataChannel) {
            DataChannel.Init i = new DataChannel.Init();
            i.ordered = true;
            mDataChannel = mLocalPeer.createDataChannel("events", i);
        }
        // Create a local media stream and attach it to the peer connection.
        MediaStream stream = mPeerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(mLocalAudioTrack);
        mLocalPeer.addStream(stream);
    }

    private void handleVideoCallAccepted() {
        Log.d(TAG, "handling video call accepted");
        Activity activity = requireActivity();
        if (activity.isDestroyed() || activity.isFinishing()) {
            return;
        }

        stopSoundEffect();
        rearrangePeerViews(activity, false);

        createPeerConnection(true);
        Cache.setCallConnected();
    }

    // Handles remote SDP offer received from the peer,
    // creates a local peer connection and sends an answer to the peer.
    private void handleVideoOfferMsg(@NonNull MsgServerInfo info) {
        // Incoming call.
        if (info.payload == null) {
            Log.e(TAG, "Received RTC offer with an empty payload. Quitting");
            handleCallClose();
            return;
        }

        // Data channel should be created by the peer. Not creating one.
        createPeerConnection(false);
        //noinspection unchecked
        Map<String, Object> m = (Map<String, Object>) info.payload;
        String type = (String) m.getOrDefault("type", "");
        String sdp = (String) m.getOrDefault("sdp", "");

        //noinspection ConstantConditions
        mLocalPeer.setRemoteDescription(new CustomSdpObserver("localSetRemote"),
                new SessionDescription(SessionDescription.Type.fromCanonicalForm(type.toLowerCase()), sdp));

        mLocalPeer.createAnswer(new CustomSdpObserver("localCreateAns") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                mLocalPeer.setLocalDescription(new CustomSdpObserver("localSetLocal"), sessionDescription);

                handleSendAnswer(sessionDescription);

                CallFragment.this.initialSetupComplete();
            }
        }, new MediaConstraints());
    }

    // Passes remote SDP received from the peer to the peer connection.
    private void handleVideoAnswerMsg(@NonNull MsgServerInfo info) {
        if (info.payload == null) {
            Log.e(TAG, "Received RTC answer with an empty payload. Quitting. ");
            handleCallClose();
            return;
        }
        //noinspection unchecked
        Map<String, Object> m = (Map<String, Object>) info.payload;
        String type = (String) m.getOrDefault("type", "");
        String sdp = (String) m.getOrDefault("sdp", "");

        //noinspection ConstantConditions
        mLocalPeer.setRemoteDescription(new CustomSdpObserver("localSetRemote"),
                new SessionDescription(SessionDescription.Type.fromCanonicalForm(type.toLowerCase()), sdp));
        initialSetupComplete();
    }

    // Adds remote ICE candidate data received from the peer to the peer connection.
    private void handleNewICECandidateMsg(@NonNull MsgServerInfo info) {
        if (info.payload == null) {
            // Skip.
            Log.e(TAG, "Received ICE candidate message an empty payload. Skipping.");
            return;
        }
        //noinspection unchecked
        Map<String, Object> m = (Map<String, Object>) info.payload;
        String sdpMid = (String) m.getOrDefault("sdpMid", "");
        //noinspection ConstantConditions
        int sdpMLineIndex = (int) m.getOrDefault("sdpMLineIndex", 0);
        String sdp = (String) m.getOrDefault("candidate", "");
        if (sdp == null || sdp.isEmpty()) {
            // Skip.
            Log.e(TAG, "Invalid ICE candidate with an empty candidate SDP" + info);
            return;
        }

        IceCandidate candidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
        if (mCallInitialSetupComplete) {
            mLocalPeer.addIceCandidate(candidate);
        } else {
            addRemoteIceCandidateToCache(candidate);
        }
    }

    // Sends a local ICE candidate to the other party.
    private void handleIceCandidateEvent(IceCandidate candidate) {
        mTopic.videoCallICECandidate(mCallSeqID,
                new IceCandidateAux("candidate", candidate.sdpMLineIndex, candidate.sdpMid, candidate.sdp));
    }

    // Cleans up call after receiving a remote hang-up notification.
    private void handleRemoteHangup(MsgServerInfo info) {
        handleCallClose();
    }

    private void playSoundEffect(@RawRes int effectId) {
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer.create(getContext(), effectId);
            mMediaPlayer.setLooping(true);
            mMediaPlayer.start();
        }
    }

    private synchronized void stopSoundEffect() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void rearrangePeerViews(final Activity activity, boolean remoteVideoLive) {
        activity.runOnUiThread(() -> {
            if (remoteVideoLive) {
                ConstraintSet cs = new ConstraintSet();
                cs.clone(mLayout);
                cs.removeFromVerticalChain(R.id.peerName);
                cs.connect(R.id.peerName, ConstraintSet.BOTTOM, R.id.callControlsPanel, ConstraintSet.TOP, 0);
                cs.setHorizontalBias(R.id.peerName, 0.05f);

                cs.applyTo(mLayout);
                mPeerName.setElevation(8);

                mPeerAvatar.setVisibility(View.INVISIBLE);
            } else {
                ConstraintSet cs = new ConstraintSet();
                cs.clone(mLayout);
                cs.removeFromVerticalChain(R.id.peerName);
                cs.connect(R.id.peerName, ConstraintSet.BOTTOM, R.id.imageAvatar, ConstraintSet.TOP, 0);
                cs.setHorizontalBias(R.id.peerName, 0.5f);
                cs.applyTo(mLayout);
                mPeerAvatar.setVisibility(View.VISIBLE);
            }
        });
    }

    // Auxiliary class to facilitate serialization of SDP data.
    static class SDPAux implements Serializable {
        public final String type;
        public final String sdp;

        SDPAux(String type, String sdp) {
            this.type = type;
            this.sdp = sdp;
        }
    }

    // Auxiliary class to facilitate serialization of the ICE candidate data.
    static class IceCandidateAux implements Serializable {
        public String type;
        public int sdpMLineIndex;
        public String sdpMid;
        public String candidate;

        IceCandidateAux(String type, int sdpMLineIndex, String sdpMid, String candidate) {
            this.type = type;
            this.sdpMLineIndex = sdpMLineIndex;
            this.sdpMid = sdpMid;
            this.candidate = candidate;
        }
    }

    // Listens for incoming call-related info messages.
    private class InfoListener implements Tinode.EventListener {
        @Override
        public void onInfoMessage(MsgServerInfo info) {
            if (MsgServerInfo.parseWhat(info.what) != MsgServerInfo.What.CALL) {
                // We are only interested in "call" info messages.
                return;
            }
            MsgServerInfo.Event event = MsgServerInfo.parseEvent(info.event);
            switch (event) {
                case ACCEPT:
                    handleVideoCallAccepted();
                    break;
                case ANSWER:
                    handleVideoAnswerMsg(info);
                    break;
                case ICE_CANDIDATE:
                    handleNewICECandidateMsg(info);
                    break;
                case HANG_UP:
                    handleRemoteHangup(info);
                    break;
                case OFFER:
                    handleVideoOfferMsg(info);
                    break;
                case RINGING:
                    playSoundEffect(R.raw.call_out);
                    break;
                default:
                    break;
            }
        }
    }

    private static class CustomSdpObserver implements SdpObserver {
        private String tag;

        CustomSdpObserver(String logTag) {
            tag = getClass().getCanonicalName();
            this.tag = this.tag + " " + logTag;
        }

        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.d(tag, "onCreateSuccess() called with: sessionDescription = [" + sessionDescription + "]");
        }

        @Override
        public void onSetSuccess() {
            Log.d(tag, "onSetSuccess() called");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.d(tag, "onCreateFailure() called with: s = [" + s + "]");
        }

        @Override
        public void onSetFailure(String s) {
            Log.d(tag, "onSetFailure() called with: s = [" + s + "]");
        }
    }

    private class FailureHandler extends UiUtils.ToastFailureListener {
        FailureHandler(Activity activity) {
            super(activity);
        }

        @Override
        public PromisedReply<ServerMessage> onFailure(final Exception err) {
            handleCallClose();
            return super.onFailure(err);
        }
    }
}
