/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.specification;

import bliss.lib.framework.util.StringUtils;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ClanInfo;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Phong
 */
public class ClanSpecification {

    public static Specification<ClanInfo> withName(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        } else {
            // Specification using Java 8 lambdas
            return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%");
        }
    }
}
