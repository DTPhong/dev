package cc.bliss.match3.service.gamemanager.ent.common;

import cc.bliss.match3.service.gamemanager.ent.enums.TicketStatus;
import lombok.Data;

import java.util.List;

@Data
public class TicketEvent {
    private int eventId;
    private int eventType;
    private long userId;
    private String userName;
    private int currentTrophy;
    private int deltaTrophy;
    private int roomId;
    private TicketStatus status;
    private long initTime;
    private long eventStartTime;
    private long eventEndTime;
    private int botType;
    private List<EventReward> listReward;

}
