/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.ent.persistence.match3;

import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.ent.common.ShardEnt;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Phong
 */
@Entity
@Table(name = "inventory")
@Data
@DynamicUpdate
public class InventoryEnt implements Serializable {

    @Id
    private long id;

    private String heroArr = "[]";

    private String shardArr = "[]";

    private String itemArr = "[]";

    public String getHero(){
        return heroArr;
    }

    public String getShard(){
        return shardArr;
    }

    public void addHero(int heroID, int level) {
        JsonArray jsonArray = getHeroArr();
        for (JsonElement jsonElement : jsonArray) {
            if (jsonElement.getAsJsonObject().get("id").getAsInt() == heroID) {
                return;
            }
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", heroID);
        jsonObject.addProperty("level", level);
        jsonArray.add(jsonObject);

        heroArr = jsonArray.toString();
    }

    public void addShard(int heroID, long shardAmount) {
        List<ShardEnt> list = getShardArr();
        if (list.stream().anyMatch(e -> e.getHeroID() == heroID)) {
            for (ShardEnt shardEnt : list) {
                if (shardEnt.getHeroID() == heroID) {
                    shardEnt.setAmount(shardEnt.getAmount() + shardAmount);
                }
            }
        } else {
            ShardEnt shardEnt = new ShardEnt();
            shardEnt.setHeroID(heroID);
            shardEnt.setAmount(shardAmount);
            list.add(shardEnt);
        }
        shardArr = JSONUtil.Serialize(list);
    }

    public long getShard(int heroID) {
        List<ShardEnt> list = getShardArr();
        if (list.stream().anyMatch(e -> e.getHeroID() == heroID)) {
            for (ShardEnt shardEnt : list) {
                if (shardEnt.getHeroID() == heroID) {
                    return shardEnt.getAmount();
                }
            }
        }
        return 0;
    }

    public void levelUpHero(int heroID, int deltaLevel) {
        JsonArray jsonArray = getHeroArr();
        for (JsonElement jsonElement : jsonArray) {
            JsonObject hero = jsonElement.getAsJsonObject();
            if (hero.get("id").getAsInt() == heroID) {
                int level = hero.get("level").getAsInt();
                hero.addProperty("level", level + deltaLevel);
            }
        }
        heroArr = jsonArray.toString();
    }

    public void levelUpSkill(int heroID, int deltaLevel) {
        JsonArray jsonArray = getHeroArr();
        for (JsonElement jsonElement : jsonArray) {
            JsonObject hero = jsonElement.getAsJsonObject();
            if (hero.get("id").getAsInt() == heroID) {
                int level = hero.has("skillLevel") ? hero.get("skillLevel").getAsInt() : 0;
                hero.addProperty("skillLevel", level + deltaLevel);
            }
        }
        heroArr = jsonArray.toString();
    }

    public List<Integer> getHeroIdArr() {
        JsonArray jsonArray = getHeroArr();
        List<Integer> integers = new ArrayList<>();
        for (JsonElement jsonElement : jsonArray) {
            integers.add(jsonElement.getAsJsonObject().get("id").getAsInt());
        }
        return integers;
    }

    public JsonArray getHeroArr() {
        if (StringUtils.isNotBlank(heroArr)) {
            return JSONUtil.DeSerialize(heroArr, JsonArray.class);
        }
        return new JsonArray();
    }

    public List<ShardEnt> getShardArr() {
        if (StringUtils.isNotBlank(shardArr)) {
            Type listType = new TypeToken<ArrayList<ShardEnt>>() {
            }.getType();
            return JSONUtil.DeSerialize(shardArr, listType);
        }
        return Collections.EMPTY_LIST;
    }

    public JsonArray getItemArr() {
        if (StringUtils.isNotBlank(itemArr)) {
            return JSONUtil.DeSerialize(itemArr, JsonArray.class);
        }
        return new JsonArray();
    }
}
