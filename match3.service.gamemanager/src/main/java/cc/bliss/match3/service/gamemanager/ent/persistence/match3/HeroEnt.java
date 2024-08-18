/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.ent.common.UpgradeConfigEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.EHeroClass;
import cc.bliss.match3.service.gamemanager.ent.enums.EHeroRarity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.reflect.TypeToken;
import lombok.Data;

import javax.persistence.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Phong
 */
@Entity
@Table(name = "heros")
@Data
public class HeroEnt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String title;
    private String description;
    private int power;
    private int hp;
    private int manaCost;
    private EHeroClass heroClass;
    private EHeroRarity rarity;
    private int atkLevelPercent;
    private int armorLevelPercent;
    private int shield;
    private String upgradeConfig = "[]";
    private String skillConfig = "[]";
    private int faction;

    @Transient
    private int level;
    @Transient
    private boolean isUsed;
    @Transient
    private boolean isOwned;
    @Transient
    private long curPiece;
    @Transient
    private int trophy;
    @Transient
    private int skillLevel;

    public void setTransientField(int level, boolean isUsed, boolean isOwned, long curPiece, int trophy, int skillLevel) {
        this.level = level;
        this.isUsed = isUsed;
        this.isOwned = isOwned;
        this.curPiece = curPiece;
        this.trophy = trophy;
        this.skillLevel = skillLevel;
    }

    public UpgradeConfigEnt getLevelUpgradeConfig(int level) {
        Type listType = new TypeToken<ArrayList<UpgradeConfigEnt>>() {
        }.getType();
        List<UpgradeConfigEnt> upgradeConfigEnts = JSONUtil.DeSerialize(upgradeConfig, listType);
        return upgradeConfigEnts.stream().anyMatch(e -> e.getLevel() == level)
                ? upgradeConfigEnts.stream().filter(e -> e.getLevel() == level).findAny().get()
                : new UpgradeConfigEnt();
    }

    public UpgradeConfigEnt getDefaultLeveConfig() {
        Type listType = new TypeToken<ArrayList<UpgradeConfigEnt>>() {
        }.getType();
        List<UpgradeConfigEnt> upgradeConfigEnts = JSONUtil.DeSerialize(upgradeConfig, listType);
        return upgradeConfigEnts.stream().findFirst().get();
    }

    public UpgradeConfigEnt getSkillUpgradeConfig(int level) {
        Type listType = new TypeToken<ArrayList<UpgradeConfigEnt>>() {
        }.getType();
        List<UpgradeConfigEnt> upgradeConfigEnts = JSONUtil.DeSerialize(skillConfig, listType);
        return upgradeConfigEnts.stream().anyMatch(e -> e.getLevel() == level)
                ? upgradeConfigEnts.stream().filter(e -> e.getLevel() == level).findAny().get()
                : new UpgradeConfigEnt();
    }

    public int getMaxLevel(int currentLevel, int currentShard) {
        Type listType = new TypeToken<ArrayList<UpgradeConfigEnt>>() {}.getType();
        List<UpgradeConfigEnt> upgradeConfigEnts = JSONUtil.DeSerialize(upgradeConfig, listType);

        int maxLevel = currentLevel;
        int remainingShard = currentShard;

        int startIndex = 0;
        for (int i = 0; i < upgradeConfigEnts.size(); i++) {
            if (upgradeConfigEnts.get(i).getLevel() == currentLevel) {
                startIndex = i;
                break;
            }
        }

        for (int i = startIndex; i < upgradeConfigEnts.size(); i++) {
            UpgradeConfigEnt config = upgradeConfigEnts.get(i + 1);
            if (config.getShard() <= remainingShard) {
                maxLevel = config.getLevel();
                remainingShard -= config.getShard();
                if (maxLevel == 10) {
                    break;
                }
            } else {
                break;
            }
        }
        return maxLevel;
    }

    public int getRemainShard(int currentLevel, int currentShard) {
        Type listType = new TypeToken<ArrayList<UpgradeConfigEnt>>() {}.getType();
        List<UpgradeConfigEnt> upgradeConfigEnts = JSONUtil.DeSerialize(upgradeConfig, listType);

        int maxLevel = currentLevel;
        int remainingShard = currentShard;


        int startIndex = 0;
        for (int i = 0; i < upgradeConfigEnts.size(); i++) {
            if (upgradeConfigEnts.get(i).getLevel() == currentLevel) {
                startIndex = i;
                break;
            }
        }

        for (int i = startIndex; i < upgradeConfigEnts.size(); i++) {
            UpgradeConfigEnt config = upgradeConfigEnts.get(i+1);
            if (config.getShard() <= remainingShard) {
                maxLevel = config.getLevel();
                remainingShard -= config.getShard();
                if (maxLevel == 10) {
                    break;
                }
            } else {
                break;
            }
        }
        return remainingShard;
    }

    public int getPower() {
        if (level <= 1) {
            return power;
        }
        return power + (power * atkLevelPercent * (level - 1) / 1000);
    }

    public int getHp() {
        if (level <= 1) {
            return hp;
        }
        return hp + (hp * atkLevelPercent * (level - 1) / 1000);
    }
}
