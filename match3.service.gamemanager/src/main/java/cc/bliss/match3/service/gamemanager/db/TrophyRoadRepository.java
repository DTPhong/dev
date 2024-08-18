package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.TrophyRoadWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.TrophyRoadReadRepository;
import org.springframework.stereotype.Component;

@Component
public class TrophyRoadRepository{

private final TrophyRoadWriteRepository write;

private final TrophyRoadReadRepository read;

public TrophyRoadRepository(TrophyRoadWriteRepository write, TrophyRoadReadRepository read) {
    this.write = write;
    this.read = read;
}

public TrophyRoadWriteRepository write(){
    return write;
}

public TrophyRoadReadRepository read(){
    return read;
}
}
