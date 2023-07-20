package id.bluebird.chat.methods.message.utils

import android.os.Handler
import android.os.Looper
import android.os.Message
import id.bluebird.chat.methods.message.MessageActivity
import java.lang.ref.WeakReference
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Utility class to send messages queued while offline.
 * The execution is paused while the activity is in background and unpaused
 * when the topic subscription is live.
 */
class PausableSingleThreadExecutor :
    ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue()) {

    private val pauseLock = ReentrantLock()
    private val unpaused = pauseLock.newCondition()
    private var isPaused = false

    override fun beforeExecute(t: Thread, r: Runnable) {
        super.beforeExecute(t, r)
        pauseLock.lock()
        try {
            while (isPaused) unpaused.await()
        } catch (ie: InterruptedException) {
            t.interrupt()
        } finally {
            pauseLock.unlock()
        }
    }

    fun pause() {
        pauseLock.lock()
        isPaused = try {
            true
        } finally {
            pauseLock.unlock()
        }
    }

    fun resume() {
        pauseLock.lock()
        try {
            isPaused = false
            unpaused.signalAll()
        } finally {
            pauseLock.unlock()
        }
    }
}

// Handler which sends "read" notifications for received messages.
class NoteHandler constructor(activity: MessageActivity) :
    Handler(Looper.getMainLooper()) {
    val ref: WeakReference<MessageActivity>

    init {
        ref = WeakReference(activity)
    }

    override fun handleMessage(msg: Message) {
        val activity = ref.get()
        val mTopic = activity?.mTopic

        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            return
        }

        if (mTopic == null) {
            return
        }

        // If messages fragment is not visible don't send the notification.
        if (!activity.isFragmentVisible(MessageActivity.FRAGMENT_MESSAGES)) {
            return
        }

        // Don't send a notification if more notifications are pending. This avoids the case of acking
        // every {data} message in a large batch.
        // It may pose a problem if a later message is acked first (msg[1].seq > msg[2].seq), but that
        // should not happen.
        if (hasMessages(0)) {
            return
        }

        val topicName = msg.obj as String

        if (topicName == mTopic.name) {
            mTopic.noteRead(msg.arg1)
        }
    }
}
