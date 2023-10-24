package id.bluebird.chat

enum class NotifPipeline(val value: String) {
    APNS("A"),
    FCM("F"),
    HUAWEI("H")
}