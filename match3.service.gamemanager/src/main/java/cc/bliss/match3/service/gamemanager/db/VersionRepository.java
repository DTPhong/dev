package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3_read.VersionReadRepository;
import org.springframework.stereotype.Component;

@Component
public class VersionRepository {

    private final VersionReadRepository readRepository;

    public VersionRepository(VersionReadRepository readRepository) {
        this.readRepository = readRepository;
    }

    public VersionReadRepository read(){
        return readRepository;
    }
}
