package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.HeroWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.HeroReadRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class HeroRepository {

    private final HeroWriteRepository write;

    private final HeroReadRepository read;

    public HeroRepository(HeroWriteRepository write, HeroReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public HeroWriteRepository write(){
        return write;
    }

    public HeroReadRepository read(){
        return read;
    }
}
