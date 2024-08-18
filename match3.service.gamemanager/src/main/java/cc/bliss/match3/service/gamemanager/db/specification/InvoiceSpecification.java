/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.db.specification;

import cc.bliss.match3.service.gamemanager.ent.persistence.match3.InvoiceEnt;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Phong
 */
public class InvoiceSpecification {

    public static Specification<InvoiceEnt> withStatus(int status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<InvoiceEnt> withProductType(int productType) {
        return (root, query, cb) -> cb.equal(root.get("productType"), productType);
    }
}
