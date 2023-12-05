package id.bluebird.chat.io.model

import grpc.Chatservice

class GenerateTokenResp {

    var token: Token = Token()

    fun setItem(response: Chatservice.GenerateTokenResponse) {
        token = Token(
            token = response.accessToken,
            refreshToken = "",
            expiredIn = response.expiresIn
        )
    }
}