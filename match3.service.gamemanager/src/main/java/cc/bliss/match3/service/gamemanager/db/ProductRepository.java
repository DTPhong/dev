package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.ProductWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.ProductReadRepository;
import org.springframework.stereotype.Component;

@Component
public class ProductRepository {

    private final ProductWriteRepository write;

    private final ProductReadRepository read;

    public ProductRepository(ProductWriteRepository write, ProductReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public ProductWriteRepository write(){
        return write;
    }

    public ProductReadRepository read(){
        return read;
    }
}
