package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.ConfigWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.ConfigReadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class ConfigRepository {

    private final ConfigWriteRepository write;

    private final ConfigReadRepository read;

    public ConfigRepository(ConfigWriteRepository write, ConfigReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public ConfigWriteRepository write(){
        return write;
    }

    public ConfigReadRepository read(){
        return read;
    }
}
