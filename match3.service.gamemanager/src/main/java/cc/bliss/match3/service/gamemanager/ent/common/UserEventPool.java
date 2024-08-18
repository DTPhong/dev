package cc.bliss.match3.service.gamemanager.ent.common;

import java.util.ArrayList;
import java.util.List;

public class UserEventPool {

    private final List<Long> userIds = new ArrayList<>();

    public void addUser(long userId) {
        userIds.add(userId);
    }

    public List<Long> getUserPool() {
        return userIds;
    }
}
