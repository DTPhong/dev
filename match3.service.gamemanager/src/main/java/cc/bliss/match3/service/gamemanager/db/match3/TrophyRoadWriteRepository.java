/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TrophyRoadMileStoneEnt;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

/**
 * @author Phong
 */
@Repository
public interface TrophyRoadWriteRepository extends WriteRepository<TrophyRoadMileStoneEnt, Integer> {

    @Transactional
    @Modifying
    void deleteByType(int type);

    @CacheEvict(value = "trophy_road", cacheManager = "cacheManagerList")
    TrophyRoadMileStoneEnt saveAndFlush(TrophyRoadMileStoneEnt var1);
}
