package cc.bliss.match3.service.gamemanager.localqueue.cmd;

import bliss.lib.framework.queue.QueueCommand;
import cc.bliss.match3.service.gamemanager.ent.enums.EQuestType;
import cc.bliss.match3.service.gamemanager.service.event.DailyQuestService;

public class ListenClaimFeatureCmd implements QueueCommand {

    DailyQuestService dailyQuestService;
    long userID;
    EQuestType questType;
    long quantity;

    public ListenClaimFeatureCmd(DailyQuestService dailyQuestService, long userID, EQuestType questType, long quantity) {
        this.dailyQuestService = dailyQuestService;
        this.quantity = quantity;
        this.questType = questType;
        this.userID = userID;
    }

    @Override
    public void execute() {
        dailyQuestService.listenClaimFeature(userID, questType , quantity);
    }
}
