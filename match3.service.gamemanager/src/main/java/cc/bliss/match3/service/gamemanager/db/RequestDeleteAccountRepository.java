package cc.bliss.match3.service.gamemanager.db;

import cc.bliss.match3.service.gamemanager.db.match3.RequestDeleteAccountWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.RequestDeleteAccountReadRepository;
import org.springframework.stereotype.Component;

@Component
public class RequestDeleteAccountRepository {

    private final RequestDeleteAccountWriteRepository write;

    private final RequestDeleteAccountReadRepository read;

    public RequestDeleteAccountRepository(RequestDeleteAccountWriteRepository write, RequestDeleteAccountReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public RequestDeleteAccountWriteRepository write(){
        return write;
    }

    public RequestDeleteAccountReadRepository read(){
        return read;
    }
}
