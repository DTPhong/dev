/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.specification;

import cc.bliss.match3.service.gamemanager.ent.enums.EProductStatus;
import cc.bliss.match3.service.gamemanager.ent.enums.EProductType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ProductEnt;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Phong
 */
public class ProductSpecification {
    public static Specification<ProductEnt> withID(int id) {
        return (root, query, cb) -> cb.equal(root.get("id"), id);
    }

    public static Specification<ProductEnt> withStatus(EProductStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status.ordinal());
    }

    public static Specification<ProductEnt> withProductType(EProductType productType) {
        return (root, query, cb) -> cb.equal(root.get("productType"), productType.ordinal());
    }
}
