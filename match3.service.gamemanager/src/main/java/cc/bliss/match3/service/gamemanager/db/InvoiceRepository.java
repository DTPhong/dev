package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.InvoiceWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.InvoiceReadRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class InvoiceRepository {

    private final InvoiceWriteRepository write;

    private final InvoiceReadRepository read;

    public InvoiceRepository(InvoiceWriteRepository write, InvoiceReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public InvoiceWriteRepository write(){
        return write;
    }

    public InvoiceReadRepository read(){
        return read;
    }
}
