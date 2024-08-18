/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.match3_read;

import cc.bliss.match3.service.gamemanager.ent.enums.EHeroRarity;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.HeroEnt;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Phong
 */
@Repository
public interface HeroReadRepository extends JpaRepository<HeroEnt, Integer> {

    HeroEnt findByTitle(String title);

    List<HeroEnt> findAll();

    Optional<HeroEnt> findById(int id);

    List<HeroEnt> findAllByRarityEquals(EHeroRarity rarity);
}
