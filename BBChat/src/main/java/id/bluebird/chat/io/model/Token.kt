package id.bluebird.chat.io.model

data class Token(
    var token: String? = null,
    var refreshToken: String? = null,
    var expiredIn: Long? = null
)