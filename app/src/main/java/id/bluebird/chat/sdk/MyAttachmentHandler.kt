package id.bluebird.chat.sdk

import android.graphics.Bitmap
import co.tinode.tinodesdk.PromisedReply
import co.tinode.tinodesdk.model.ServerMessage
import id.bluebird.chat.sdk.media.VxCard

class MyAttachmentHandler {
    companion object {
        fun uploadAvatar(pub: VxCard, bmp: Bitmap?, topicName: String?): PromisedReply<ServerMessage<*, *, *, *>?>? {
            return AttachmentHandler.uploadAvatar(pub, null, "newacc")
        }
    }
}
