/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.common;

import cc.bliss.match3.service.gamemanager.ent.enums.ERewardType;
import cc.bliss.match3.service.gamemanager.util.RandomItem;
import cc.bliss.match3.service.gamemanager.util.RandomUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Phong
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardEnt {

    // reponse item
    ERewardType eRewardType = ERewardType.NONE;
    int ref = 0;
    long delta = 0;
    long before = 0;
    long after = 0;

    // config item
    int slot;
    int orgin;
    int bound;
    //for reward type
    int ownedPercentRewardType;
    //for hero
    int notOwnedPercent;
    List<RandomItem> randomItems;
    boolean adsRequire = false;

    /**
     * Config for gold
     *
     * @param slot
     * @param orgin
     * @param bound
     */
    public RewardEnt(int slot, int orgin, int bound) {
        this.slot = slot;
        this.eRewardType = ERewardType.GOLD;
        this.orgin = orgin;
        this.bound = bound;
    }

    /**
     * Config for shard
     *
     * @param slot
     * @param randomItems
     * @param quantity
     */
    public RewardEnt(int slot, List<RandomItem> randomItems, int quantity) {
        this.slot = slot;
        this.eRewardType = ERewardType.HERO_CARD;
        this.randomItems = randomItems;
        this.delta = quantity;
    }

    public String getTitle() {
        switch (eRewardType) {
            case HERO:
                return "Tướng";
            case HERO_CARD:
                return "Mảnh tướng";
            case GOLD:
            case PACK_GOLD_1:
            case PACK_GOLD_2:
            case PACK_GOLD_3:
            case PACK_GOLD_4:
            case PACK_GOLD_5:
            case PACK_GOLD_6:
                return "Gold";
            case TUB_OF_AMETHYST:
            case CASKET_OF_AMETHYST:
            case AMETHYST:
                return "Amethyst";
            case WOODEN_CHEST:
                return "WOODEN CHEST";
            case SLIVER_CHEST:
                return "SLIVER CHEST";
            case GOLDEN_CHEST:
                return "GOLDEN CHEST";
            case SHINY_JEWEL_CHEST:
                return "SHINY JEWEL CHEST";
            case BLACK_CROWN_CHEST:
                return "BLACK CROWN CHEST";
            case BEGINNER_CHEST:
                return "BEGINNER CHEST";
            case ADS_CHEST:
                return "ADS CHEST";
            case EMERALD:
            case COMMON_EMERALD:
            case PACK_EMERALD_1:
            case PACK_EMERALD_2:
            case PACK_EMERALD_3:
            case PACK_EMERALD_4:
            case PACK_EMERALD_5:
            case PACK_EMERALD_6:
                return "EMERALD";
            case ROYAL_AMETHYST:
                return "ROYAL AMETHYST";
            case RARE_CARD:
                return "RARE CARD";
            case EPIC_CARD:
                return "EPIC CARD";
            case LEGENDARY_CARD:
                return "LEGENDARY CARD";
            default:
                return "Chưa mô tả";
        }
    }

    public String getDescription() {
        switch (eRewardType) {
            case HERO:
                return "Tướng";
            case HERO_CARD:
                return "Mảnh tướng";
            case GOLD:
            case PACK_GOLD_1:
            case PACK_GOLD_2:
            case PACK_GOLD_3:
            case PACK_GOLD_4:
            case PACK_GOLD_5:
            case PACK_GOLD_6:
                return "Gold";
            default:
                return "Chưa mô tả";
        }
    }

    public String getImg() {
        switch (eRewardType) {
            default:
                return "https://static.kplay.mobi/tet2021/event_tet_icon_ncoin.png";
        }
    }

    public List<RewardEnt> getRandomItem() {
        List<RewardEnt> list = new ArrayList<>();
        switch (eRewardType) {
            case GOLD: {
                RewardEnt itemEnt = new RewardEnt();
                itemEnt.setERewardType(eRewardType);
                itemEnt.setDelta(RandomUtils.random(orgin, bound));
                list.add(itemEnt);
            }
            break;
            case HERO_CARD: {
                for (int i = 0; i < delta; i++) {
                    RewardEnt itemEnt = new RewardEnt();
                    itemEnt.setERewardType(eRewardType);
                    itemEnt.setDelta(1);
                    itemEnt.setRef(RandomUtils.random(randomItems));
                    list.add(itemEnt);
                }
            }
            break;
        }
        return list;
    }
}
