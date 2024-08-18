package cc.bliss.match3.service.gamemanager.ent.common;

import lombok.Data;

import java.util.List;

@Data
public class RoomEvent {
    private int id;
    private int eventType;
    private long startEventTime;
    private long endEventTime;
    private long startRoomTime;
    private long endRoomTime;
    private boolean isSendResult = false;
    private List<UserParticipant> listUser;
    private List<EventReward> listReward;

}
