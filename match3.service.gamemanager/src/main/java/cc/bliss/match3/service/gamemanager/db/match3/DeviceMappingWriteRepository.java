package cc.bliss.match3.service.gamemanager.db.match3;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.DeviceMappingEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.DeviceMappingID;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceMappingWriteRepository extends WriteRepository<DeviceMappingEnt, DeviceMappingID> {

    @CachePut(value = "device_mapping",key = "{#id?.deviceID,#id?.socialID}", cacheManager = "cacheManagerObject")
    DeviceMappingEnt saveAndFlush(DeviceMappingEnt var1);
}
