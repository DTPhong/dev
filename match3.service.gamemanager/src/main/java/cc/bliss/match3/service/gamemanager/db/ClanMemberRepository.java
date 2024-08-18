package cc.bliss.match3.service.gamemanager.db;


import cc.bliss.match3.service.gamemanager.db.match3.ClanMemberWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3_read.ClanMemberReadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class ClanMemberRepository {

    private final ClanMemberWriteRepository write;

    private final ClanMemberReadRepository read;

    public ClanMemberRepository(ClanMemberWriteRepository write, ClanMemberReadRepository read) {
        this.write = write;
        this.read = read;
    }

    public ClanMemberWriteRepository write(){
        return write;
    }

    public ClanMemberReadRepository read(){
        return read;
    }
}
