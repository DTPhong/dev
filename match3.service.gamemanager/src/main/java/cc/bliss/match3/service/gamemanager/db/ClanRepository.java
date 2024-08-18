package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.ClanWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.ClanReadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class ClanRepository {

    private final ClanWriteRepository write;

    private final ClanReadRepository read;

    public ClanRepository(ClanWriteRepository write, ClanReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public ClanWriteRepository write(){
        return write;
    }

    public ClanReadRepository read(){
        return read;
    }
}
