package cc.bliss.match3.service.gamemanager.service.system;

import java.io.Serializable;
import java.util.Map;

public class DefaultMessageDelegate implements MessageDelegate {
    @Override
    public void handleMessage(String message) {
    }

    @Override
    public void handleMessage(Map message) {
    }

    @Override
    public void handleMessage(byte[] message) {
    }

    @Override
    public void handleMessage(Serializable message) {
        TicketService.IS_MAINTAIN = true;
    }

    @Override
    public void handleMessage(Serializable message, String channel) {
    }
}
