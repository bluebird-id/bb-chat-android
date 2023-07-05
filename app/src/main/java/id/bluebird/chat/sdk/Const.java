package id.bluebird.chat.sdk;

public class Const {
    public static final int ACTION_UPDATE_SELF_SUB = 0;
    public static final int ACTION_UPDATE_SUB = 1;
    public static final int ACTION_UPDATE_AUTH = 2;
    public static final int ACTION_UPDATE_ANON = 3;

    public static final String INTENT_EXTRA_TOPIC = "id.bluebird.chat.TOPIC";

    // Maximum length of user name or topic title.
    public static final int MAX_TITLE_LENGTH = 60;
    // Maximum length of topic description.
    public static final int MAX_DESCRIPTION_LENGTH = 360;

    // Maximum linear dimensions of images.
    static final int MAX_BITMAP_SIZE = 1024;
    public static final int AVATAR_THUMBNAIL_DIM = 36; // dip

    // Image preview size in messages.
    public static final int IMAGE_PREVIEW_DIM = 64;
    public static final int MIN_AVATAR_SIZE = 8;
    public static final int MAX_AVATAR_SIZE = 384;
    // Maximum byte size of avatar sent in-band.
    public static final int MAX_INBAND_AVATAR_SIZE = 4096;
}
