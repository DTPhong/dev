package cc.bliss.match3.service.gamemanager.db.match3_read;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Version;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VersionReadRepository extends ReadOnlyRepository<Version, String> {

    @Cacheable(value = "version_optional", unless = "#result == null", cacheManager = "cacheManagerObject")
    Optional<Version> findById(String id);
}
