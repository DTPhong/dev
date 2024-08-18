package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.TriggerWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.TriggerReadRepository;
import org.springframework.stereotype.Component;

@Component
public class TriggerRepository {

    private final TriggerWriteRepository write;

    private final TriggerReadRepository read;

    public TriggerRepository(TriggerWriteRepository write, TriggerReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public TriggerWriteRepository write(){
        return write;
    }

    public TriggerReadRepository read(){
        return read;
    }
}
