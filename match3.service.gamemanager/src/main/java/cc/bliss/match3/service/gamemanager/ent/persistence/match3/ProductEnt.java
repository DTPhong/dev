/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

/**
 * @author Phong
 */
@Entity
@Table(name = "product")
@Data
@DynamicUpdate
public class ProductEnt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String productKey;
    private int productType;
    private int amount;
    private int amountEmerald;
    private int gold;
    private int diamond;
    private int status;

    private String title;
    private int iconID;
    private String tag;
}
