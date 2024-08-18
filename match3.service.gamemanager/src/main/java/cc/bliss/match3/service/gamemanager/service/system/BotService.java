package cc.bliss.match3.service.gamemanager.service.system;

import org.springframework.stereotype.Service;

@Service
public class BotService {

    public boolean isBotTutorial(long botID){
        return botID == 1 || botID == 2 || botID == 3;
    }

    public int getBotType(long botID){
        return 0;
    }
}
