package id.bluebird.chat.sdk;

public class Const {
    public static final String FCM_TOKEN = "token";
    public static final String FCM_REFRESH_TOKEN = "FCM_REFRESH_TOKEN";
    public static final String PREF_TYPING_NOTIF = "pref_typingNotif";
    public static final String PREF_READ_RCPT = "pref_readReceipts";

    public static final String INTENT_ACTION_CALL_CLOSE = "bluebird.intent.action.call.CLOSE";

    public static final String INTENT_EXTRA_OTHER_NAME_CHAT = "id.bluebird.chat.OTHER_NAME_CHAT";
    public static final String INTENT_EXTRA_USER_TYPE = "id.bluebird.chat.USER_TYPE";

    public static final String INTENT_EXTRA_TOPIC_CHAT = "id.bluebird.chat.TOPIC_CHAT";
    public static final String INTENT_EXTRA_TOPIC_CALL = "id.bluebird.chat.TOPIC_CALL";
    public static final String INTENT_EXTRA_SEQ = "id.bluebird.chat.SEQ";
    public static final String INTENT_EXTRA_CALL_DIRECTION = "id.bluebird.chat.CALL_DIRECTION";
    public static final String INTENT_EXTRA_CALL_ACCEPTED = "id.bluebird.chat.CALL_ACCEPTED";

    // Length of quoted text when replying.
    public static final int QUOTED_REPLY_LENGTH = 64;

    // Maximum linear dimensions of images.
    static final int MAX_BITMAP_SIZE = 1024;
    public static final int AVATAR_THUMBNAIL_DIM = 36; // dip
    // Image thumbnail in quoted replies and reply/forward previews.
    public static final int REPLY_THUMBNAIL_DIM = 36;
    // Width of video thumbnail in quoted replies and reply/forward previews.
    public static final int REPLY_VIDEO_WIDTH = 48;

    // Image preview size in messages.
    public static final int IMAGE_PREVIEW_DIM = 64;
    public static final int MIN_AVATAR_SIZE = 8;
    public static final int MAX_AVATAR_SIZE = 384;
    // Maximum byte size of avatar sent in-band.
    public static final int MAX_INBAND_AVATAR_SIZE = 4096;

    public static final String CALL_NOTIFICATION_CHAN_ID = "video_calls";
    public static final String NEWMSG_NOTIFICATION_CHAN_ID = "new_message";

}
