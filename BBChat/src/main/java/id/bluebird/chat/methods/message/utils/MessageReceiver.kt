package id.bluebird.chat.methods.message.utils

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import id.bluebird.chat.R
import id.bluebird.chat.methods.message.MessageActivity
import id.bluebird.chat.methods.message.MessageActivity.Companion.TAG
import java.io.File
import java.net.URI

val MessageActivity.onDownloadComplete: BroadcastReceiver
    get() = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            var intentMutable = intent
            val dm: DownloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager

            val downloadId = intentMutable.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == -1L) {
                return
            }

            val query = DownloadManager.Query()
            query.setFilterById(downloadId)

            val c = dm.query(query)
            if (c.moveToFirst()) {
                var idx = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = if (idx >= 0) {
                    c.getInt(idx)
                } else {
                    -1
                }

                if (DownloadManager.STATUS_SUCCESSFUL == status) {
                    idx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    val fileUri = if (idx >= 0) {
                        URI.create(c.getString(idx))
                    } else {
                        null
                    }

                    idx = c.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)
                    val mimeType = if (idx >= 0) {
                        c.getString(idx)
                    } else {
                        null
                    }

                    if (fileUri != null) {
                        intentMutable = Intent()
                        intentMutable.action = Intent.ACTION_VIEW
                        intentMutable.setDataAndType(
                            FileProvider.getUriForFile(
                                this@onDownloadComplete,
                                "id.bluebird.chat.provider", File(fileUri)
                            ), mimeType
                        )
                        intentMutable.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                        try {
                            startActivity(intentMutable)
                        } catch (ignored: ActivityNotFoundException) {
                            Log.w(TAG, "No application can view downloaded file")
                            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                        }
                    }
                } else if (DownloadManager.STATUS_FAILED == status) {
                    idx = c.getColumnIndex(DownloadManager.COLUMN_REASON)
                    val reason = if (idx >= 0) c.getInt(idx) else -1
                    Log.w(TAG, "Download failed. Reason: $reason")

                    Toast.makeText(
                        this@onDownloadComplete,
                        R.string.failed_to_download, Toast.LENGTH_SHORT
                    ).show()
                }
            }
            c.close()
        }
    }

val onNotificationClick: BroadcastReceiver
    get() = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // FIXME: handle notification click.
            Log.d(TAG, "onNotificationClick" + intent.extras)
        }
    }