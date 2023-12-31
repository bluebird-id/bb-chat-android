package id.bluebird.chat.methods.message

import android.app.DownloadManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import co.tinode.tinodesdk.ComTopic
import co.tinode.tinodesdk.PromisedReply
import co.tinode.tinodesdk.PromisedReply.FinalListener
import co.tinode.tinodesdk.Tinode
import co.tinode.tinodesdk.model.Drafty
import co.tinode.tinodesdk.model.ServerMessage
import id.bluebird.chat.BBChat
import id.bluebird.chat.R
import id.bluebird.chat.methods.message.utils.MessageEventListener
import id.bluebird.chat.methods.message.utils.NoteHandler
import id.bluebird.chat.methods.message.utils.PausableSingleThreadExecutor
import id.bluebird.chat.methods.message.utils.changeTopic
import id.bluebird.chat.methods.message.utils.isFragmentVisible
import id.bluebird.chat.methods.message.utils.onDownloadComplete
import id.bluebird.chat.methods.message.utils.onNotificationClick
import id.bluebird.chat.methods.message.utils.readOtherNameFromIntent
import id.bluebird.chat.methods.message.utils.readTopicNameCallFromIntent
import id.bluebird.chat.methods.message.utils.readTopicNameChatFromIntent
import id.bluebird.chat.methods.message.utils.readUserTypeFromIntent
import id.bluebird.chat.sdk.AttachmentHandler
import id.bluebird.chat.sdk.Cache
import id.bluebird.chat.sdk.CallManager
import id.bluebird.chat.sdk.Const
import id.bluebird.chat.sdk.FilePreviewFragment
import id.bluebird.chat.sdk.UiUtils
import id.bluebird.chat.sdk.UiUtils.ToastFailureListener
import id.bluebird.chat.sdk.account.Utils
import id.bluebird.chat.sdk.db.BaseDb
import id.bluebird.chat.sdk.db.SqlStore
import id.bluebird.chat.sdk.demos.InvalidTopicFragment
import id.bluebird.chat.sdk.demos.message.MessagesFragment
import id.bluebird.chat.sdk.demos.message.UserType
import id.bluebird.chat.sdk.demos.previewmedia.ImageViewFragment
import id.bluebird.chat.sdk.media.VxCard
import java.util.Timer

/**
 * View to display a single conversation
 */
class MessageActivity : AppCompatActivity() {

    lateinit var mMessageSender: PausableSingleThreadExecutor

    // Handler for sending {note what="read"} notifications after a READ_DELAY.
    lateinit var mNoteReadHandler: Handler

    private var mTinodeListener: MessageEventListener? = null

    // Notification settings.
    private var mSendTypingNotifications = false

    var mOtherName: String? = null

    var mTopicChatName: String? = null

    var mTopicCallName: String? = null

    var mUserType: UserType? = null

    var mMessageText: String? = null

    var mTypingAnimationTimer: Timer? = null

    var mTopic: ComTopic<VxCard>? = null

    var mSendReadReceipts = false

    // Only for grp topics:
    // Keeps track of the known subscriptions for the given topic.
    var mKnownSubs: MutableSet<String>? = null

    // True when new subscriptions were added to the topic.
    var mNewSubsAvailable = false

    var chatEnabled: MutableLiveData<Boolean> = MutableLiveData(BBChat.CHAT_ENABLED)

    var fragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mTopicChatName = savedInstanceState.getString(TOPIC_CHAT_NAME)
        }
        setContentView(R.layout.activity_messages)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupReceiver()

        setupMessageSender()
    }

    private fun setupReceiver() {
        registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )

        registerReceiver(
            onNotificationClick,
            IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun setupMessageSender() {
        mMessageSender = PausableSingleThreadExecutor()

        mMessageSender.pause()
        mNoteReadHandler = NoteHandler(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mTopic == null || !mTopic!!.isValid) {
            return false
        }

        return if (item.itemId == R.id.action_audio_call) {
            CallManager.placeOutgoingCall(this, mTopicCallName)
            true
        }else if(item.itemId == android.R.id.home){
            finish()
            false
        } else {
            false
        }
    }

    public override fun onResume() {
        super.onResume()

        // Intent with parameters passed on start of the activity.
        val intent = intent
        saveIntentExtraText(intent)

        setupChatHistoryListener()

        // If topic name is not saved, get it from intent, internal or external.
        var topicName = mTopicChatName
        if (TextUtils.isEmpty(mTopicChatName)) {
            topicName = readTopicNameChatFromIntent(intent)
            mTopicCallName = readTopicNameCallFromIntent(intent)
            mOtherName = readOtherNameFromIntent(intent)
            mUserType = readUserTypeFromIntent(intent)
        }
        Log.e("BBChat", topicName.toString())
        if (!changeTopic(topicName, false)) {
            Cache.setSelectedTopicName(null)
            finish()
            return
        }

        // Resume message sender.
        mMessageSender.resume()

        handleAttachmentFile()

        setupPreferencesData()

        chatEnabled.observe(this) {
            if (fragment is MessagesFragment) {
                (fragment as MessagesFragment).enabledChat(it)
            }
        }
    }

    // Extra Text from or for Image Caption or Forward Message and Fill to Edittext
    private fun saveIntentExtraText(intent: Intent) {
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
        mMessageText = if (TextUtils.isEmpty(text)) {
            null
        } else {
            text.toString()
        }
        intent.putExtra(Intent.EXTRA_TEXT, null as String?)
    }

    private fun setupChatHistoryListener() {
        val tinode = Cache.getTinode()
        mTinodeListener = MessageEventListener(
            activity = this,
            online = tinode.isConnected
        )
        tinode.addListener(mTinodeListener)
    }

    private fun handleAttachmentFile() {
        val attachment = intent.data
        val type = intent.type
        if (attachment != null && type != null && Utils.MIME_TINODE_PROFILE != type) {
            // Need to retain access right to the given Uri.
            val args = Bundle()
            args.putParcelable(AttachmentHandler.ARG_LOCAL_URI, attachment)
            args.putString(AttachmentHandler.ARG_MIME_TYPE, type)

            if (type.startsWith("image/")) {
                args.putString(AttachmentHandler.ARG_IMAGE_CAPTION, mMessageText)
                showFragment(FRAGMENT_VIEW_IMAGE, args, true)
            } else if (type.startsWith("video/")) {
                args.putString(AttachmentHandler.ARG_IMAGE_CAPTION, mMessageText)
                showFragment(FRAGMENT_VIEW_VIDEO, args, true)
            } else {
                showFragment(FRAGMENT_FILE_PREVIEW, args, true)
            }
        }
        intent.data = null
    }

    private fun setupPreferencesData() {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        mSendReadReceipts = pref.getBoolean(Const.PREF_READ_RCPT, true)
        mSendTypingNotifications = pref.getBoolean(Const.PREF_TYPING_NOTIF, true)
        BaseDb.getInstance().store.msgPruneFailed(mTopic)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(TOPIC_CHAT_NAME, mTopicChatName)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        UiUtils.setVisibleTopic(
            if (hasFocus) {
                mTopicChatName
            } else {
                null
            }
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    public override fun onPause() {
        super.onPause()
        mMessageSender.pause()
        Cache.getTinode().removeListener(mTinodeListener)
        topicDetach()

        // Stop handling read messages
        mNoteReadHandler.removeMessages(0)
    }

    // Clean up everything related to the topic being replaced of removed.
    private fun topicDetach() {
        mTypingAnimationTimer?.run {
            cancel()
            mTypingAnimationTimer = null
        }
        mTopic?.setListener(null)
        UiUtils.setVisibleTopic(null)
    }

    public override fun onDestroy() {
        super.onDestroy()
        mMessageSender.shutdownNow()
        try {
            unregisterReceiver(onDownloadComplete)
        } catch (e: java.lang.IllegalArgumentException) {
            e.printStackTrace()
        }
        try {
            unregisterReceiver(onNotificationClick)
        } catch (e: java.lang.IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    fun runMessagesLoader() {
        val fragment =
            supportFragmentManager.findFragmentByTag(FRAGMENT_MESSAGES) as MessagesFragment?
        if (fragment != null && fragment.isVisible) {
            fragment.runMessagesLoader(mTopicChatName)
        }
    }

    fun showFragment(tag: String, args: Bundle?, addToBackstack: Boolean) {
        var argsMutable = args
        var addToBackstackMutable = addToBackstack

        if (isFinishing || isDestroyed) {
            return
        }

        val fm = supportFragmentManager
        var fragment = fm.findFragmentByTag(tag)
        this.fragment = fragment

        if (fragment == null) {
            fragment = when (tag) {
                FRAGMENT_MESSAGES -> MessagesFragment()
                FRAGMENT_INVALID -> InvalidTopicFragment()
                FRAGMENT_VIEW_IMAGE -> ImageViewFragment()
                FRAGMENT_FILE_PREVIEW -> FilePreviewFragment()
                else -> throw IllegalArgumentException("Failed to create fragment: unknown tag $tag")
            }
        } else if (argsMutable == null) {
            // Retain old arguments.
            argsMutable = fragment.arguments
        }

        argsMutable = argsMutable ?: Bundle()
        argsMutable.putString(Const.INTENT_EXTRA_OTHER_NAME_CHAT, mOtherName)
        argsMutable.putString(Const.INTENT_EXTRA_TOPIC_CHAT, mTopicChatName)
        argsMutable.putSerializable(Const.INTENT_EXTRA_USER_TYPE, mUserType)

        if (tag == FRAGMENT_MESSAGES) {
            argsMutable.putString(MessagesFragment.MESSAGE_TO_SEND, mMessageText)
            mMessageText = null
        }

        if (fragment.arguments != null) {
            fragment.requireArguments().putAll(argsMutable)
        } else {
            fragment.arguments = argsMutable
        }

        var trx = fm.beginTransaction()
        if (!fragment.isAdded) {
            trx = trx.replace(R.id.contentFragment, fragment, tag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        } else if (!fragment.isVisible) {
            trx = trx.show(fragment)
        } else {
            addToBackstackMutable = false
        }

        if (FRAGMENT_MESSAGES == tag) {
            trx.setPrimaryNavigationFragment(fragment)
        }

        if (addToBackstackMutable) {
            trx.addToBackStack(tag)
        }

        if (!trx.isEmpty) {
            trx.commit()
        }
    }

    // Try to send the specified message.
    fun syncMessages(msgId: Long, runLoader: Boolean) {
        mMessageSender.submit {
//            val promise: PromisedReply<ServerMessage<*, *, *, *>> = mTopic!!.syncOne(msgId)

            val promise: PromisedReply<ServerMessage<*, *, *, *>> = if (msgId > 0) {
                mTopic!!.syncOne(msgId)
            } else {
                mTopic!!.syncAll<SqlStore.MessageList>()
            }

            if (runLoader) {
                promise.thenApply(object :
                    PromisedReply.SuccessListener<ServerMessage<*, *, *, *>?>() {
                    override fun onSuccess(result: ServerMessage<*, *, *, *>?): PromisedReply<ServerMessage<*, *, *, *>?>? {
                        runMessagesLoader()
                        return null
                    }
                })
            }
            promise.thenCatch(object : PromisedReply.FailureListener<ServerMessage<*, *, *, *>?>() {
                override fun <E : Exception?> onFailure(err: E): PromisedReply<ServerMessage<*, *, *, *>?>? {
                    Log.w(TAG, "Sync failed", err)
                    return null
                }
            })
        }
    }

    fun sendMessage(content: Drafty?, seq: Int): Boolean {
        if (mTopic != null) {
            val head = if (seq > 0) Tinode.headersForReply(seq) else null
            val done = mTopic!!.publish(content, head)
            BaseDb.getInstance().store.msgPruneFailed(mTopic)
            runMessagesLoader() // Refreshes the messages: hides removed, shows pending.
            done
                .thenApply(object : PromisedReply.SuccessListener<ServerMessage<*, *, *, *>?>() {
                    override fun onSuccess(result: ServerMessage<*, *, *, *>?): PromisedReply<ServerMessage<*, *, *, *>?>? {
                        if (mTopic!!.isArchived) {
                            mTopic!!.updateArchived(false)
                        }
                        return null
                    }
                })
                .thenCatch(ToastFailureListener(this))
                .thenFinally(object : FinalListener() {
                    override fun onFinally() {
                        // Updates message list with "delivered" or "failed" icon.
                        runMessagesLoader()
                    }
                })
            return true
        }
        return false
    }

    fun showReply(reply: Drafty?, seq: Int) {
        if (isFragmentVisible(FRAGMENT_MESSAGES)) {
            val mf =
                supportFragmentManager.findFragmentByTag(FRAGMENT_MESSAGES) as MessagesFragment?
            mf?.showReply(this, reply, seq)
        }
    }

    // Schedule a delayed {note what="read"} notification.
    fun sendNoteRead(seq: Int) {
        if (mSendReadReceipts) {
            val msg = mNoteReadHandler.obtainMessage(0, seq, 0, mTopicChatName)
            mNoteReadHandler.sendMessageDelayed(msg, MessageActivity.READ_DELAY.toLong())
        }
    }

    fun sendKeyPress() {
        if (mTopic != null && mSendTypingNotifications) {
            mTopic!!.noteKeyPress()
        }
    }

    fun sendRecordingProgress(audioOnly: Boolean) {
        if (mTopic != null && mSendTypingNotifications) {
            mTopic!!.noteRecording(audioOnly)
        }
    }

    // Try to send all pending messages.
    fun syncAllMessages(runLoader: Boolean) {
        syncMessages(-1, runLoader)
    }

    private fun enableChat(value: Boolean){
        if(chatEnabled.isInitialized.not()) {
            chatEnabled.postValue(value)
        } else {
            chatEnabled = MutableLiveData(value)
        }
    }

    companion object {
        const val TAG = "MessageActivity"
        const val FRAGMENT_MESSAGES = "msg"
        const val FRAGMENT_INVALID = "invalid"
        const val FRAGMENT_VIEW_IMAGE = "view_image"
        const val FRAGMENT_VIEW_VIDEO = "view_video"
        const val FRAGMENT_FILE_PREVIEW = "file_preview"
        const val TOPIC_CHAT_NAME = "topicChatName"
        const val MESSAGES_TO_LOAD = 24
        const val READ_DELAY = 1000

        fun setChatEnabled(value: Boolean){
            MessageActivity().enableChat(value)
        }
    }
}