package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.InventoryWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.InventoryReadRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class InventoryRepository {

    private final InventoryWriteRepository write;

    private final InventoryReadRepository read;

    public InventoryRepository(InventoryWriteRepository write, InventoryReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public InventoryWriteRepository write(){
        return write;
    }

    public InventoryReadRepository read(){
        return read;
    }
}
