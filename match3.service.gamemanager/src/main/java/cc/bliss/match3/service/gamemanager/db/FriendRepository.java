package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.FriendWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.FriendReadRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class FriendRepository {

    private final FriendWriteRepository write;

    private final FriendReadRepository read;

    public FriendRepository(FriendWriteRepository write, FriendReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public FriendWriteRepository write(){
        return write;
    }

    public FriendReadRepository read(){
        return read;
    }
}
