package id.bluebird.chat.sdk;

public class Const {
    public static final int ACTION_UPDATE_SELF_SUB = 0;
    public static final int ACTION_UPDATE_SUB = 1;
    public static final int ACTION_UPDATE_AUTH = 2;
    public static final int ACTION_UPDATE_ANON = 3;

    public static final String PREF_TYPING_NOTIF = "pref_typingNotif";
    public static final String PREF_READ_RCPT = "pref_readReceipts";

    public static final String INTENT_ACTION_CALL_CLOSE = "bluebird.intent.action.call.CLOSE";

    public static final String INTENT_EXTRA_TOPIC = "id.bluebird.chat.TOPIC";
    public static final String INTENT_EXTRA_SEQ = "id.bluebird.chat.SEQ";
    public static final String INTENT_EXTRA_CALL_DIRECTION = "co.tinode.tindroid.CALL_DIRECTION";
    public static final String INTENT_EXTRA_CALL_ACCEPTED = "id.bluebird.chat.CALL_ACCEPTED";

    // Maximum length of user name or topic title.
    public static final int MAX_TITLE_LENGTH = 60;
    // Maximum length of topic description.
    public static final int MAX_DESCRIPTION_LENGTH = 360;
    // Length of quoted text when replying.
    public static final int QUOTED_REPLY_LENGTH = 64;
    // Length of quoted text when altering a message.
    public static final int EDIT_PREVIEW_LENGTH = 64;

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
