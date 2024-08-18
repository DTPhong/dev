/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.common;

import bliss.lib.framework.common.LogUtil;
import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.JSONUtil;
import bliss.lib.framework.util.StringUtils;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.db.match3.HeroWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3.ProfileStatisticWriteRepository;
import cc.bliss.match3.service.gamemanager.ent.common.Statistic;
import cc.bliss.match3.service.gamemanager.ent.persistence.game_log.GameLog;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ProfileStatistic;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.util.GZipUtils;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
@Service
public class ProfileStatisticService extends BaseService {

    private static final String USER_STATISTIC = "ustat:%s";
    /**
     * Thời gian key tồn tại trên cache
     */
    private static final int EXPIRED_MILIS = ModuleConfig.STATISTIC_EXPIRE;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private LeaderboardService leaderboardService;

    private void saveData(long userid, String data) {
        RedisConnection connection = null;
        try {
            connection = redisTemplateString.getConnectionFactory().getConnection();

            String key = String.format(USER_STATISTIC, userid);
            byte[] compressedData = GZipUtils.compress(data.getBytes());
            connection.set(key.getBytes(), compressedData);
            connection.expire(key.getBytes(), EXPIRED_MILIS * 1000);

            Optional<ProfileStatistic> optional = profileStatisticRepository.read().findById(userid);
            ProfileStatistic profileStatistic;
            if (optional.isPresent()) {
                profileStatisticRepository.write().updateData(userid, data);
            } else {
                profileStatistic = new ProfileStatistic();
                profileStatistic.setUserID(userid);
                profileStatistic.setData(data);
                insertMatch3SchemaData(profileStatistic);
            }
        } catch (Exception e) {
            System.out.println(LogUtil.stackTrace(e));
        } finally {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }

    private String getData(long userid) {
        RedisConnection connection = null;
        try {
            connection = redisTemplateString.getConnectionFactory().getConnection();

            String key = String.format(USER_STATISTIC, userid);
            byte[] compressedData = connection.get(key.getBytes());
            connection.expire(key.getBytes(), EXPIRED_MILIS * 1000);
            if (compressedData == null) {
                return StringUtils.EMPTY;
            }
            return new String(GZipUtils.decompress(compressedData));
        } catch (Exception e) {
            System.out.println(LogUtil.stackTrace(e));
            return StringUtils.EMPTY;
        } finally {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }

    public void saveStatistic(long userid, Map<String, Statistic> map) {
        String data = JSONUtil.Serialize(map);
        saveData(userid, data);
    }

    private Map<String, Statistic> getStatisticCache(long userid) {
        String data = getData(userid);
        return StringUtils.isEmpty(data) ? null : JSONUtil.DeSerialize(data, new TypeToken<Map<String, Statistic>>() {
        }.getType());
    }

    public void clearData(long userid) {
        profileStatisticRepository.write().deleteById(userid);
    }

    public Statistic getStatistic(long userID, int heroId) {
        Map<String, Statistic> map = getStatisticCache(userID);
        if (map == null) {
            ProfileStatistic profileStatistic = profileStatisticRepository.read().findById(userID).orElse(null);
            if (profileStatistic == null) {
                map = new HashMap<>();
            } else {
                map = JSONUtil.DeSerialize(profileStatistic.getData(), HashMap.class);
            }
            saveStatistic(userID, map);
        }
        Statistic statistic = map.containsKey(heroId) ? map.get(heroId) : new Statistic();
        return statistic;
    }

    public Map<String, Statistic> getStatisticData(long userID) {
        Map<String, Statistic> map = getStatisticCache(userID);
        if (map == null) {
            ProfileStatistic profileStatistic = profileStatisticRepository.read().findById(userID).orElse(null);
            if (profileStatistic == null) {
                map = new HashMap<>();
            } else {
                Type type = new TypeToken<HashMap<String, Statistic>>() {}.getType();
                map = JSONUtil.DeSerialize(profileStatistic.getData(), type);
            }
            saveStatistic(userID, map);
        }
        List<Integer> listHeroIDs = map.keySet().stream().map(e -> ConvertUtils.toInt(e)).collect(Collectors.toList());
        Map<Integer, Integer> mapTrophy = leaderboardService.getHeroTrophy(userID, listHeroIDs);
        for (Statistic value : map.values()) {
            value.setTrophy(mapTrophy.getOrDefault(value.getHeroID(), 0));
        }
        return map;
    }

    public void recordStatistic(GameLog gameLog) {
        long winID = gameLog.getWinID();
        List<Long> userIDs = gameLog.getListUserID();
        for (Long userID : userIDs) {
            Profile profile = profileService.getMinProfileByID(userID);
            int selectHero = profile.getSelectHero();
            String heroName = heroRepository.read().findById(selectHero).get().getDescription();
            Map<String, Statistic> mapStatistic = getStatisticData(userID);
            Statistic statistic = mapStatistic.getOrDefault(String.valueOf(selectHero), new Statistic());
            statistic.setHeroID(selectHero);
            statistic.setHeroName(heroName);
            statistic.setTotalHand(statistic.getTotalHand() + 1);
            if (winID == userID) {
                statistic.setWinHand(statistic.getWinHand() + 1);
            }
            float winRate = ((float) statistic.getWinHand() / statistic.getTotalHand()) * 100;
            DecimalFormat decimalFormat = new DecimalFormat("#");
            String winRateFormat = decimalFormat.format(winRate) + "%";
            statistic.setWinRate(winRateFormat);
            mapStatistic.put(String.valueOf(selectHero), statistic);
            saveStatistic(userID, mapStatistic);
        }
    }
}
