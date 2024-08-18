/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.system;

import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.ent.common.RoomEnt;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Phong
 */
@Service
public class RoomService extends BaseService {

    private final String ROOM_ID_KEY = "room_id";

    public int getBlankRoom() {
        return redisTemplateString.opsForValue().increment(ROOM_ID_KEY).intValue();
    }
}
