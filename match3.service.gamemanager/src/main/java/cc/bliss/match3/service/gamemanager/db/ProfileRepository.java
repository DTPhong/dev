package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.ProfileWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.ProfileReadRepository;
import org.springframework.stereotype.Component;

@Component
public class ProfileRepository {

    private final ProfileWriteRepository write;

    private final ProfileReadRepository read;

    public ProfileRepository(ProfileWriteRepository write, ProfileReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public ProfileWriteRepository write(){
        return write;
    }

    public ProfileReadRepository read(){
        return read;
    }
}
