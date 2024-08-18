package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.QuestPoolWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.QuestPoolReadRepository;
import org.springframework.stereotype.Component;

@Component
public class QuestPoolRepository {

    private final QuestPoolWriteRepository write;

    private final QuestPoolReadRepository read;

    public QuestPoolRepository(QuestPoolWriteRepository write, QuestPoolReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public QuestPoolWriteRepository write(){
        return write;
    }

    public QuestPoolReadRepository read(){
        return read;
    }
}
