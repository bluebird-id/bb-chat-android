package id.bluebird.chat.io.model

import grpc.Chatservice

class ParticipantsResp {

    var participants: Participants = Participants()

    fun setItem(response: Chatservice.GetParticipantsResponse) {
        participants = Participants(
            participantsIdlist = response.participantsIdListList,
            roomId = response.roomId,
        )
    }
}
