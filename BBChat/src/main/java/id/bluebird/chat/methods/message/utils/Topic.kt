package id.bluebird.chat.methods.message.utils

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import co.tinode.tinodesdk.AlreadySubscribedException
import co.tinode.tinodesdk.ComTopic
import co.tinode.tinodesdk.NotConnectedException
import co.tinode.tinodesdk.PromisedReply
import co.tinode.tinodesdk.ServerResponseException
import co.tinode.tinodesdk.model.Description
import co.tinode.tinodesdk.model.MsgServerData
import co.tinode.tinodesdk.model.MsgServerInfo
import co.tinode.tinodesdk.model.MsgServerPres
import co.tinode.tinodesdk.model.PrivateType
import co.tinode.tinodesdk.model.ServerMessage
import co.tinode.tinodesdk.model.Subscription
import com.google.firebase.messaging.RemoteMessage
import id.bluebird.chat.methods.message.MessageActivity
import id.bluebird.chat.methods.message.MessageActivity.Companion.FRAGMENT_MESSAGES
import id.bluebird.chat.methods.message.MessageActivity.Companion.TAG
import id.bluebird.chat.sdk.Cache
import id.bluebird.chat.sdk.Const
import id.bluebird.chat.sdk.UiUtils
import id.bluebird.chat.sdk.account.Utils
import id.bluebird.chat.sdk.demos.InvalidTopicFragment
import id.bluebird.chat.sdk.demos.message.MessagesFragment
import id.bluebird.chat.sdk.demos.message.UserType
import id.bluebird.chat.sdk.media.VxCard

// Get topic name from Intent the Activity was launched with (push notification, other app, other activity).
fun MessageActivity.readTopicNameChatFromIntent(intent: Intent): String? {

    // Check if the activity was launched by internally-generated intent.
    var name = intent.getStringExtra(Const.INTENT_EXTRA_TOPIC_CHAT)
    if (!TextUtils.isEmpty(name)) {
        return name
    }

    // Check if activity was launched from a background push notification.
    val msg = intent.getParcelableExtra<RemoteMessage>("msg")
    if (msg != null) {
        val notification = msg.notification
        if (notification != null) {
            return notification.tag
        }
    }

    // mTopicName is empty, so this is an external intent
    val contactUri = intent.data
    if (contactUri != null) {
        val cursor =
            contentResolver.query(contactUri, arrayOf(Utils.DATA_PID), null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(Utils.DATA_PID)
                if (idx >= 0) {
                    name = cursor.getString(idx)
                }
            }
            cursor.close()
        }
    }
    return name
}

// Get topic name from Intent the Activity was launched with (push notification, other app, other activity).
fun readTopicNameCallFromIntent(intent: Intent): String? {

    // Check if the activity was launched by internally-generated intent.
    var name = intent.getStringExtra(Const.INTENT_EXTRA_TOPIC_CALL)
    return name
}

fun readOtherNameFromIntent(intent: Intent): String? {

    // Check if the activity was launched by internally-generated intent.
    var name = intent.getStringExtra(Const.INTENT_EXTRA_OTHER_NAME_CHAT)
    return name
}

fun readUserTypeFromIntent(intent: Intent): UserType? {

    // Check if the activity was launched by internally-generated intent.
    var userType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getSerializableExtra(Const.INTENT_EXTRA_USER_TYPE, UserType::class.java)
    } else {
        intent.getSerializableExtra(Const.INTENT_EXTRA_USER_TYPE) as UserType
    }
    return userType
}

/**
 * Process Subscribe Chat List
 */
fun MessageActivity.topicAttach() {
    if (!Cache.getTinode().isAuthenticated) {
        // If connection is not ready, wait for completion. This method will be called again
        // from the onLogin callback;
        return
    }

    setRefreshing(true)

    var builder = mTopic!!.metaGetBuilder
        .withDesc()
        .withSub()
        .withLaterData(MessageActivity.MESSAGES_TO_LOAD)
        .withDel()

    if (mTopic!!.isOwner) {
        builder = builder.withTags()
    }

    if (mTopic!!.isDeleted) {
        setRefreshing(false)
        UiUtils.setupToolbar(this, mTopic!!.pub, mTopicChatName, false, null, true, null)
        maybeShowMessagesFragmentOnAttach()
        return
    }

    mTopic!!.subscribe(null, builder.build())
        .thenApply(object : PromisedReply.SuccessListener<ServerMessage<*, *, *, *>?>() {
            override fun onSuccess(result: ServerMessage<*, *, *, *>?):
                    PromisedReply<ServerMessage<*, *, *, *>?>? {

                if (result?.ctrl != null && result.ctrl.code == 303) {
                    // Redirect.
                    changeTopic(result.ctrl.getStringParam("topic", null), false)
                    return null
                }

                runOnUiThread {
                    val fragment = maybeShowMessagesFragmentOnAttach()
                    if (fragment is MessagesFragment) {
                        UiUtils.setupToolbar(
                            this@topicAttach,
                            mTopic!!.pub,
                            mTopicChatName,
                            mTopic!!.online,
                            mTopic!!.lastSeen,
                            mTopic!!.isDeleted,
                            null
                        )
                    }
                }

                // Submit pending messages for processing: publish queued,
                // delete marked for deletion.
                syncAllMessages(true)
                return null
            }
        })
        .thenCatch(object : PromisedReply.FailureListener<ServerMessage<*, *, *, *>?>() {
            override fun <E : Exception?> onFailure(err: E): PromisedReply<ServerMessage<*, *, *, *>?>? {
                if (err !is NotConnectedException && err !is AlreadySubscribedException) {
                    Log.w(MessageActivity.TAG, "Subscribe failed", err)
                    if (err is ServerResponseException) {
                        val code = err.code
                        if (code == 404) {
                            showFragment(MessageActivity.FRAGMENT_INVALID, null, false)
                        }
                    }
                }
                return null
            }
        })
        .thenFinally(object : PromisedReply.FinalListener() {
            override fun onFinally() {
                setRefreshing(false)
            }
        })
}


// Topic has changed. Update all the views with the new data.
// Returns 'true' if topic was successfully changed, false otherwise.
fun MessageActivity.changeTopic(
    topicName: String?,
    forceReset: Boolean
): Boolean {
    if (TextUtils.isEmpty(topicName)) {
        Log.w(MessageActivity.TAG, "Failed to switch topics: empty topic name")
        return false
    }

    // Cancel all pending notifications addressed to the current topic.
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.cancel(topicName, 0)

    Log.e("BBChat", "changeTopic: $topicName")
    val tinode = Cache.getTinode()

    val topic: ComTopic<VxCard>? = try {
        tinode.getTopic(topicName) as ComTopic<VxCard>?
    } catch (ex: ClassCastException) {
        Log.w(MessageActivity.TAG, "Failed to switch topics: non-comm topic")
        return false
    }
    mTopic = topic

    var changed = false
    if (mTopicChatName == null || mTopicChatName != topicName) {
        Cache.setSelectedTopicName(topicName)
        mTopicChatName = topicName
        changed = true

        if (mTopic == null) {
            UiUtils.setupToolbar(this, null, mTopicChatName, false, null, false, null)
            mTopic = try {
                tinode.newTopic(mTopicChatName, null) as ComTopic<VxCard>
            } catch (ex: ClassCastException) {
                Log.w(MessageActivity.TAG, "New topic is a non-comm topic: $mTopicChatName")
                return false
            }
            showFragment(MessageActivity.FRAGMENT_INVALID, null, false)

            // Check if another fragment is already visible. If so, don't change it.
        } else if (forceReset || UiUtils.getVisibleFragment(supportFragmentManager) == null) {
            UiUtils.setupToolbar(
                this, mTopic!!.pub, mTopicChatName,
                mTopic!!.online, mTopic!!.lastSeen, mTopic!!.isDeleted, null
            )

            // Reset requested or no fragment is visible. Show default and clear back stack.
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            showFragment(MessageActivity.FRAGMENT_MESSAGES, null, false)
        }
    }
    mNewSubsAvailable = false
    mKnownSubs = HashSet()

    if (mTopic != null && mTopic!!.isGrpType) {
        val subs = mTopic!!.subscriptions
        if (subs != null) {
            for (sub in subs) {
                if (sub.user != null) {
                    (mKnownSubs as HashSet<String>).add(sub.user)
                }
            }
        }
    }
    if (mTopic == null) {
        return true
    }

    mTopic!!.setListener(TListener(this))
    if (!mTopic!!.isAttached) {
        // Try immediate reconnect.
        topicAttach()
    }

    val fragmsg =
        supportFragmentManager.findFragmentByTag(MessageActivity.FRAGMENT_MESSAGES) as MessagesFragment?
    fragmsg?.topicChanged(topicName, forceReset || changed)
    return true
}

class TListener internal constructor(val activity: MessageActivity) :
    ComTopic.ComListener<VxCard?>() {

    companion object {
        // How long a typing indicator should play its animation, milliseconds.
        const val TYPING_INDICATOR_DURATION = 4000
    }

    override fun onSubscribe(code: Int, text: String) {
        // Topic name may change after subscription, i.e. new -> grpXXX
        activity.mTopicChatName = activity.mTopic!!.name
    }

    override fun onData(data: MsgServerData) {
        // Don't send a notification for own messages. They are read by default.
        if (!Cache.getTinode().isMe(data.from)) {
            activity.sendNoteRead(data.seq)
        }
        // Cancel typing animation.
        activity.runOnUiThread {
            activity.mTypingAnimationTimer =
                UiUtils.toolbarTypingIndicator(activity, activity.mTypingAnimationTimer, -1)
        }
        activity.runMessagesLoader()
    }

    override fun onPres(pres: MsgServerPres) {
        // noinspection SwitchStatementWithTooFewBranches
        when (MsgServerPres.parseWhat(pres.what)) {
            MsgServerPres.What.ACS -> activity.runOnUiThread {
                val fragment = UiUtils.getVisibleFragment(activity.supportFragmentManager)
                if (fragment != null) {
                    if (fragment is DataSetChangeListener) {
                        (fragment as DataSetChangeListener).notifyDataSetChanged()
                    } else if (fragment is MessagesFragment) {
                        fragment.notifyDataSetChanged(true)
                    }
                }
            }

            else -> Log.d(
                TAG,
                "Topic '" + activity.mTopicChatName + "' onPres what='" + pres.what + "' (unhandled)"
            )
        }
    }

    override fun onInfo(info: MsgServerInfo) {
        when (MsgServerInfo.parseWhat(info.what)) {
            MsgServerInfo.What.READ, MsgServerInfo.What.RECV -> activity.runOnUiThread {
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag(FRAGMENT_MESSAGES)
                            as MessagesFragment?
                if (fragment != null && fragment.isVisible) {
                    fragment.notifyDataSetChanged(false)
                }
            }

            MsgServerInfo.What.KP -> activity.runOnUiThread {
                // Show typing indicator as animation over avatar in toolbar
                activity.mTypingAnimationTimer = UiUtils.toolbarTypingIndicator(
                    activity,
                    activity.mTypingAnimationTimer, TYPING_INDICATOR_DURATION
                )
            }

            else -> {}
        }
    }

    override fun onSubsUpdated() {
        val context = activity.applicationContext
        activity.runOnUiThread {
            val fragment = UiUtils.getVisibleFragment(activity.supportFragmentManager)
            if (fragment != null) {
                if (fragment is DataSetChangeListener) {
                    (fragment as DataSetChangeListener).notifyDataSetChanged()
                } else if (fragment is MessagesFragment) {
                    fragment.notifyDataSetChanged(true)
                    if (activity.mNewSubsAvailable) {
                        activity.mNewSubsAvailable = false
                        // Reload so we can correctly display messages from
                        // new users (subscriptions).
                        fragment.notifyDataSetChanged(false)
                    }
                }
            }
        }
    }

    override fun onMetaDesc(desc: Description<VxCard?, PrivateType>?) {
        activity.runOnUiThread {
            val fragment = UiUtils.getVisibleFragment(activity.supportFragmentManager)
            if (fragment != null) {
                if (fragment is DataSetChangeListener) {
                    (fragment as DataSetChangeListener).notifyDataSetChanged()
                } else if (fragment is MessagesFragment) {
                    UiUtils.setupToolbar(
                        activity,
                        activity.mTopic!!.pub,
                        activity.mTopic!!.name,
                        activity.mTopic!!.online,
                        activity.mTopic!!.lastSeen,
                        activity.mTopic!!.isDeleted,
                        null
                    )
                    fragment.notifyDataSetChanged(true)
                }
            }
        }
    }

    override fun onMetaSub(sub: Subscription<VxCard?, PrivateType>?) {
        if (activity.mTopic!!.isGrpType && sub?.user != null && !activity.mKnownSubs!!.contains(sub.user)) {
            activity.mKnownSubs!!.add(sub.user)
            activity.mNewSubsAvailable = true
        }
    }

    override fun onContUpdate(sub: Subscription<VxCard?, PrivateType>?) {
        onMetaDesc(null)
    }

    override fun onMetaTags(tags: Array<String>) {
        activity.runOnUiThread {
            val fragment = UiUtils.getVisibleFragment(activity.supportFragmentManager)
            if (fragment is DataSetChangeListener) {
                (fragment as DataSetChangeListener).notifyDataSetChanged()
            }
        }
    }

    override fun onOnline(online: Boolean) {
        activity.runOnUiThread {
            UiUtils.toolbarSetOnline(
                activity,
                activity.mTopic!!.online, activity.mTopic!!.lastSeen
            )
        }
    }
}

internal interface DataSetChangeListener {
    fun notifyDataSetChanged()
}

/**
 * Show progress indicator based on current status
 *
 * @param active should be true to show progress indicator
 */
fun MessageActivity.setRefreshing(active: Boolean) {
    if (isFinishing || isDestroyed) {
        return
    }
    runOnUiThread {
        val fragMsg = supportFragmentManager
            .findFragmentByTag(MessageActivity.FRAGMENT_MESSAGES) as MessagesFragment?
        fragMsg?.setRefreshing(active)
    }
}

fun MessageActivity.maybeShowMessagesFragmentOnAttach(): Fragment? {
    val fm = supportFragmentManager
    val visible = UiUtils.getVisibleFragment(fm)
    if (visible is InvalidTopicFragment) {
        // Replace InvalidTopicFragment with default FRAGMENT_MESSAGES.
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        showFragment(MessageActivity.FRAGMENT_MESSAGES, null, false)
    } else {
        val fragmsg = fm.findFragmentByTag(MessageActivity.FRAGMENT_MESSAGES) as MessagesFragment?
        fragmsg?.topicChanged(mTopicChatName, true)
    }
    return visible
}
