package cc.bliss.match3.service.gamemanager.db.match3;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.RequestDeleteAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestDeleteAccountWriteRepository extends WriteRepository<RequestDeleteAccount, Long> {
}
