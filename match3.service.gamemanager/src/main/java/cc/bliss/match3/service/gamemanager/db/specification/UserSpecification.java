/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.specification;

import bliss.lib.framework.util.StringUtils;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Phong
 */
public class UserSpecification {

    public static Specification<Profile> withUserID(int userID) {
        if (userID == 0) {
            return null;
        } else {
            // Specification using Java 8 lambdas
            return (root, query, cb) -> cb.equal(root.get("id"), userID);
        }
    }

    public static Specification<Profile> withUsername(String username) {
        if (StringUtils.isEmpty(username)) {
            return null;
        } else {
            // Specification using Java 8 lambdas
            return (root, query, cb) -> cb.like(root.get("username"), "%" + username + "%");
        }
    }
}
