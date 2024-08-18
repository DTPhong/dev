package cc.bliss.match3.service.gamemanager.db.match3_read;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileReadRepository extends ReadOnlyRepository<Profile, Long> {

    Optional<Profile> findByUsername(String username);

    Optional<Profile> findByGoogleId(String ggID);

    Optional<Profile> findByDeviceID(String ggID);

    Optional<Profile> findByAppleID(String appleID);

    Boolean existsByUsername(String username);

    List<Profile> findAllByBotTypeIsNotAndSelectHeroIn(int botType, Collection<Integer> selectHero);

}
