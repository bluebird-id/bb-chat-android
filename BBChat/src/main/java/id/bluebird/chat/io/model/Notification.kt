package id.bluebird.chat.io.model

data class Notification(
    val content: String? = null,
    val from: String? = null,
    val mime: String? = null,
    val seq: Int? = null,
    val silent: Boolean? = null,
    val topic: String? = null,
    val ts: String? = null,
    val what: String? = null
)