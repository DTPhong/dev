package cc.bliss.match3.service.gamemanager.localqueue.cmd;

import bliss.lib.framework.queue.QueueCommand;
import cc.bliss.match3.service.gamemanager.service.event.Login7dQuestService;
import lombok.AllArgsConstructor;

/*
* Lắng nghe các update FTUE step
* */
@AllArgsConstructor
public class TutorialChangeCmd  implements QueueCommand {

    private long userId;

    private Login7dQuestService login7dQuestService;

    @Override
    public void execute(){
        login7dQuestService.listenTutorialEvent(userId);
    }
}
