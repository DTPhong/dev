package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.MailWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.MailReadRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class MailRepository {

    private final MailWriteRepository write;

    private final MailReadRepository read;

    public MailRepository(MailWriteRepository write, MailReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public MailWriteRepository write(){
        return write;
    }

    public MailReadRepository read(){
        return read;
    }
}
