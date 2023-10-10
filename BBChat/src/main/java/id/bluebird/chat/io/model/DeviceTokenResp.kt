package id.bluebird.chat.io.model

import grpc.Chatservice

class DeviceTokenResp {

    var message: String = ""

    fun setItem(response: Chatservice.SaveDeviceTokenResponse) {
        message = response.stateMessage
    }
}