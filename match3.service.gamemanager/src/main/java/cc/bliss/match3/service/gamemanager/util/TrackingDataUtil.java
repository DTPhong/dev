package cc.bliss.match3.service.gamemanager.util;

import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.ent.data.UserDetect;
import org.springframework.data.redis.core.RedisTemplate;

public class TrackingDataUtil {

    public static UserDetect getUserDetect(long userId, RedisTemplate<String, String> redisTemplateString) {
        String sessionKey = String.format(GameConstant.CLIENT_INFO, userId);
        String data = redisTemplateString.opsForValue().get(sessionKey);
        if (data == null) {
            return new UserDetect("-","-");
        }
        return ModuleConfig.GSON_BUILDER.fromJson(data, UserDetect.class);
    }
}
