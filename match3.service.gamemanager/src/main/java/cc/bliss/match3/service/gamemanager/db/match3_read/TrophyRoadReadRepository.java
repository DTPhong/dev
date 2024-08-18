/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3_read;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TrophyRoadMileStoneEnt;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * @author Phong
 */
@Repository
public interface TrophyRoadReadRepository extends ReadOnlyRepository<TrophyRoadMileStoneEnt, Integer> {

    @Cacheable(value = "trophy_road", cacheManager = "cacheManagerList")
    List<TrophyRoadMileStoneEnt> findAll(@Nullable Specification<TrophyRoadMileStoneEnt> var1, Sort var2);
}
