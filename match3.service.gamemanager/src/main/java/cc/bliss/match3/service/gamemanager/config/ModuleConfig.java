/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.config;

import bliss.lib.framework.common.Config;
import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.DateTimeUtils;
import bliss.lib.framework.util.StringUtils;
import cc.bliss.match3.service.gamemanager.util.GoogleUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.dozer.DozerBeanMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
public class ModuleConfig {

    /**
     * Server_id = region_id + pod_name
     */
    public static final String SERVER_ID = String.format("%s_%s", GoogleUtils.getRegionName(), System.getenv("HOSTNAME"));
    public static final String SERVICE_CONFIG = "service_config";
    public static final String SPRING_BOOT_CONFIG = Config.getParam("spring_boot_config", "conf_path");
    public static final String K8S_SERVER_CONFIG = Config.getParam("k8s_config", "k8s_server");
    public static final String K8S_KEY_JSON_PATH = Config.getParam("k8s_config", "key_json_path");
    public static final String AGONES_GAMESERVER_NAME = Config.getParam("k8s_config", "gameserver_name");
    public static final int EVENT_DURATION_TIME = ConvertUtils.toInt(Config.getParam("event_config", "time_duration"));

    public static final String REDIS_CONFIG = "redis_config";

    public static final Calendar CALENDAR_INSTANCE = Calendar.getInstance();

    public static final boolean IS_TEST = ConvertUtils.toBoolean(Config.getParam("spring_boot_config", "is_test"), false);
    public static final boolean IS_DEBUG = ConvertUtils.toBoolean(Config.getParam("spring_boot_config", "is_debug"), true);
    public static final int SESSION_EXPIRED_TIME = ConvertUtils.toInt(Config.getParam("spring_boot_config", "session_expired"), 30);
    public static final String WHITELIST_IPs = ConvertUtils.toString(Config.getParam("service_config", "whitelist_ip"));
    public static final List<String> WHITELIST_IP = StringUtils.isEmpty(WHITELIST_IPs)
            ? Collections.EMPTY_LIST
            : Arrays.stream(WHITELIST_IPs.split("_")).collect(Collectors.toList());
    public static final String DATE_TIME_FORMAT = "HH:mm:ss-dd/MM/yyyy";
    public static final String CLOSE_TEST_START_TIME_STRING = ConvertUtils.toString(Config.getParam("service_config", "close_test_start_time"));
    public static final Date CLOSE_TEST_START_TIME = DateTimeUtils.getDateTime(CLOSE_TEST_START_TIME_STRING, DATE_TIME_FORMAT);

    public static final int DATA_LIMIT = ConvertUtils.toInt(Config.getParam("spring_boot_config", "data_limit"), 10);
    public static final Gson GSON_BUILDER = new GsonBuilder().create();
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final DozerBeanMapper DOZER_MAPPER = new DozerBeanMapper();

    public static final String STRING_IMG_STORAGE_PATH = Config.getParam("service_config", "img_storage_path");
    public static final String STRING_IMG_STORAGE_URL = Config.getParam("service_config", "img_storage_url");
    public static final int HERO_DEFAULT_ID = ConvertUtils.toInt(Config.getParam("hero_config", "default_id"), 2);

    public static final int MESSAGE_LIMIT = ConvertUtils.toInt(Config.getParam("chat_config", "message_limit"), 20);
    private static final String TROPHY_ROAD_START_CONFIG = ConvertUtils.toString(Config.getParam("trophy_road", "start_time"), "00:00:00-01/01/2024");
    private static final String TROPHY_ROAD_END_CONFIG = ConvertUtils.toString(Config.getParam("trophy_road", "end_time"), "00:00:00-20/02/2024");
    public static final long TROPHY_ROAD_END_TIME_MILLISECOND = DateTimeUtils.getMiliseconds(DateTimeUtils.getDateTime(TROPHY_ROAD_END_CONFIG, DATE_TIME_FORMAT));
    public static final long TROPHY_ROAD_START_TIME_MILLISECOND = DateTimeUtils.getMiliseconds(DateTimeUtils.getDateTime(TROPHY_ROAD_START_CONFIG, DATE_TIME_FORMAT));
    public static int STATISTIC_EXPIRE = ConvertUtils.toInt(Config.getParam("service_config", "statistic_expired"), (3 * 3600));
    public static int ONE_DAY_SECOND = 24 * 3600;
    public static int ONE_WEEK_SECOND = 7 * 24 * 3600;
    public static int ONE_MONTH_SECOND = 30 * 24 * 3600;
    public static final String SSE_URL = ConvertUtils.toString(Config.getParam("sse_service", "url"), "https://sse-dev.match3arena.com");

    public static final String ANDROID_REFRESH_TOKEN = ConvertUtils.toString(Config.getParam("service_config", "android_refresh_token"));
    public static final String ANDROID_CLIENT_ID = ConvertUtils.toString(Config.getParam("service_config", "android_client_id"));
    public static final String ANDROID_CLIENT_SECRET = ConvertUtils.toString(Config.getParam("service_config", "android_client_secret"));
}
