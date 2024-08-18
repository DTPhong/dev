package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.EventWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.EventReadRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class EventRepository {

    private final EventWriteRepository write;

    private final EventReadRepository read;

    public EventRepository(EventWriteRepository write, EventReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public EventWriteRepository write(){
        return write;
    }

    public EventReadRepository read(){
        return read;
    }
}
