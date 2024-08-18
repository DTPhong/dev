/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.game_log;

import cc.bliss.match3.service.gamemanager.ent.persistence.game_log.GameLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * @author Phong
 */
@Repository
public interface GameLogRepository extends JpaRepository<GameLog, Long>, JpaSpecificationExecutor<GameLog> {

}
