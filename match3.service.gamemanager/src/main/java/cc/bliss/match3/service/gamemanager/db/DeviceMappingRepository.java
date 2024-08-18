package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.DeviceMappingWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.DeviceMappingReadRepository;
import org.springframework.stereotype.Component;

@Component
public class DeviceMappingRepository {

    private final DeviceMappingWriteRepository write;

    private final DeviceMappingReadRepository read;

    public DeviceMappingRepository(DeviceMappingWriteRepository write, DeviceMappingReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public DeviceMappingWriteRepository write(){
        return write;
    }

    public DeviceMappingReadRepository read(){
        return read;
    }
}