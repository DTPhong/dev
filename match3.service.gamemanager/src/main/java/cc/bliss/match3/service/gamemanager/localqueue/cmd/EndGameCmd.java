/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.localqueue.cmd;

import bliss.lib.framework.queue.QueueCommand;
import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.ent.persistence.game_log.GameLog;
import cc.bliss.match3.service.gamemanager.service.common.ProfileStatisticService;
import cc.bliss.match3.service.gamemanager.service.event.DailyQuestService;
import cc.bliss.match3.service.gamemanager.service.event.QuestEventService;
import cc.bliss.match3.service.gamemanager.service.event.RushArenaService;
import cc.bliss.match3.service.gamemanager.service.event.WinBattleService;
import cc.bliss.match3.service.gamemanager.service.system.GameLogService;
import cc.bliss.match3.service.gamemanager.service.system.RoomService;

/**
 * @author Phong
 */
public class EndGameCmd implements QueueCommand {

    GameLog gameLog;
    RoomService roomService;
    ProfileStatisticService profileStatisticService;
    QuestEventService eventService;
    DailyQuestService dailyQuestService;
    RushArenaService rushArenaService;
    WinBattleService winBattleService;

    public EndGameCmd(GameLog gameLog, RoomService roomService,
                      ProfileStatisticService profileStatisticService,
                      QuestEventService eventService, DailyQuestService dailyQuestService,
                      RushArenaService rushArenaService, WinBattleService winBattleService) {
        this.gameLog = gameLog;
        this.roomService = roomService;
        this.profileStatisticService = profileStatisticService;
        this.eventService = eventService;
        this.dailyQuestService = dailyQuestService;
        this.rushArenaService = rushArenaService;
        this.winBattleService = winBattleService;
    }

    @Override
    public void execute() {
//        ticketService.leaveRoom(gameLog.getListUserID());
        profileStatisticService.recordStatistic(gameLog);

        if (gameLog.getWinID() != 0) {
            int gameCount = GameLogService.MAP_GAME_COUNT.merge(gameLog.getWinID(), 1, Integer::sum);
            if (gameCount >= GameConstant.GAME_ROUND_REWARD) {
                GameLogService.MAP_GAME_COUNT.remove(gameLog.getWinID());
            }
        }
        eventService.listenEndGame(gameLog);
        dailyQuestService.listenEndGame(gameLog);
        rushArenaService.listenEndGame(gameLog);
        winBattleService.listenEndGame(gameLog);
    }

}
