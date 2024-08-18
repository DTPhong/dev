package cc.bliss.match3.service.gamemanager.ent.common;

import lombok.Data;

@Data
public class GameServerDO {
    private String address;
    private int port;
    private int maxRoom;
    private int currentRom;
    private int availableRoom;
    private String state;
    private String gameServerName;
    private String gameServerId;
}
