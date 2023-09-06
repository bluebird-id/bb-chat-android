package id.bluebird.chat.io.model

import grpc.Chatservice

class ParticipantsResp {

    var participants: Participants = Participants()

    fun setItem(response: Chatservice.GetParticipantsResponse) {
        participants = Participants(
            chatRoomId = response.chatRoomId,
            callRoomId = response.callRoomId,
        )
    }
}
