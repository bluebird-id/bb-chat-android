package id.bluebird.chat.io.model

import grpc.Chatservice

class RegisterResp {

    var message: String = ""

    fun setItem(response: Chatservice.RegisterResponse) {
        message = response.stateMessage
    }
}