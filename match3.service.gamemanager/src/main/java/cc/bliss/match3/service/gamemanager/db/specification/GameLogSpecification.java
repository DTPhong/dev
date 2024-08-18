/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.specification;

import cc.bliss.match3.service.gamemanager.ent.persistence.game_log.GameLog;
import org.springframework.data.jpa.domain.Specification;

import java.util.Date;

/**
 * @author Phong
 */
public class GameLogSpecification {

    public static Specification<GameLog> withDateBetween(Date from, Date to) {
        return (root, query, cb) -> cb.between(root.get("createdTime"), from, to);
    }

    public static Specification<GameLog> withDurationBetween(long from, long to) {
        return (root, query, cb) -> cb.between(root.get("duration"), from, to);
    }

    public static Specification<GameLog> withRoomID(int roomID) {
        if (roomID == 0) {
            return null;
        } else {
            // Specification using Java 8 lambdas
            return (root, query, cb) -> cb.equal(root.get("roomID"), roomID);
        }
    }

    public static Specification<GameLog> withPlayerID(long userID) {
        if (userID == 0) {
            return null;
        } else {
            // Specification using Java 8 lambdas
            return (root, query, cb) -> cb.like(root.get("playerIDS"), "%" + userID + "%");
        }
    }
}
