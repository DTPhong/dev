package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.ProfileStatisticWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.ProfileStatisticReadRepository;
import org.springframework.stereotype.Component;

@Component
public class ProfileStatisticRepository {

    private final ProfileStatisticWriteRepository write;

    private final ProfileStatisticReadRepository read;

    public ProfileStatisticRepository(ProfileStatisticWriteRepository write, ProfileStatisticReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public ProfileStatisticWriteRepository write(){
        return write;
    }

    public ProfileStatisticReadRepository read(){
        return read;
    }
}
