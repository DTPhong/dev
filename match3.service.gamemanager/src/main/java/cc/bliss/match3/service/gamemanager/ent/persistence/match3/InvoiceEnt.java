/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

/**
 * @author Phong
 */
@Entity
@Table(name = "invoice")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamicUpdate
public class InvoiceEnt {

    public String userName;
    public String userIp;
    public int userGroup;
    public String sourceName;
    public String marketingCampaign;
    public String appPackageId;
    public String appPlatform;
    public long diamondBefore;
    public long userRetention;
    public int productId;
    public String productKey;
    public int productType;
    public int amount;
    public long diamondPaid;
    public long refId;
    public int promotionType;
    public int promotionRate;
    public long nCoinPromotion;
    public String localCampaign;
    public int status;
    public String data;
    public String partnerId;
    public byte partnerStatus;
    public long purchasedDate;
    public long completedDate;
    public String isoCurrencyCode;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private long userId;
}
