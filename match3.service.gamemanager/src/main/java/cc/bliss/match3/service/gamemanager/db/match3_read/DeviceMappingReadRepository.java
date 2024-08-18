package cc.bliss.match3.service.gamemanager.db.match3_read;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.DeviceMappingEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.DeviceMappingID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceMappingReadRepository extends ReadOnlyRepository<DeviceMappingEnt, DeviceMappingID> {

    @Query(value = "select * from device_mapping where socialid = :socialID", nativeQuery = true)
    List<DeviceMappingEnt> findBySocialID(@Param(value = "socialID") String socialID);

    @Cacheable(value = "device_mapping",key = "{#id?.deviceID,#id?.socialID}", unless = "#result == null", cacheManager = "cacheManagerObject")
    Optional<DeviceMappingEnt> findById(DeviceMappingID id);


}
