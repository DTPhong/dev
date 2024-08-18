package cc.bliss.match3.service.gamemanager.service.common;

import bliss.lib.framework.util.DateTimeUtils;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.config.RestTemplateConfig;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.db.ProfileRepository;
import cc.bliss.match3.service.gamemanager.ent.common.GameServerDO;
import cc.bliss.match3.service.gamemanager.ent.common.TicketEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.TicketService;
import cc.bliss.match3.service.gamemanager.util.GoogleUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AgonesService extends BaseService {

    private final RestTemplate restTemplate;
    private final AdminService adminService;
    private final ProfileRepository profileRepository;

    private static final int MIN_AVAILABLE_ROOM_TO_ALLOCATE = 70;
    private static final int BUFFER_ROOM = 50;

    public AgonesService(RestTemplate restTemplate, AdminService adminService, ProfileRepository profileRepository) {
        this.restTemplate = restTemplate;
        this.adminService = adminService;
        this.profileRepository = profileRepository;
    }

    public String getRegion(){
        JsonObject response = new JsonObject();
        response.addProperty("region", GoogleUtils.getRegionName());
        long userId = adminService.getSession().getId();
        setProcessTime(userId, response);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, NetWorkAPI.LOGIN);
    }

    private void setProcessTime(long userId, JsonObject response) {
        //read db time
        long beforeReadDBTime = System.currentTimeMillis();
        Profile profile = profileRepository.read().getById(userId);
        long afterReadDBTime = System.currentTimeMillis();
        response.addProperty("readDBTimeMs", afterReadDBTime - beforeReadDBTime);
        //write db time
        long beforeWriteDBTime = System.currentTimeMillis();
        profileRepository.write().testWriteDB(profile.getId());
        long afterWriteDBTime = System.currentTimeMillis();
        response.addProperty("writeDBTimeMs", afterWriteDBTime - beforeWriteDBTime);
        //read redis time
        long beforeReadRedisTime = System.currentTimeMillis();
        redisTemplateString.opsForValue().get("client_info_"+userId);
        long afterReadRedisTime = System.currentTimeMillis();
        response.addProperty("readRedisTimeMs", afterReadRedisTime - beforeReadRedisTime);
        //write redis time
        long beforeWriteRedisTime = System.currentTimeMillis();
        redisTemplateString.opsForValue().set("multi_region_check_time", "0", 60000, TimeUnit.MILLISECONDS);
        long afterWriteRedisTime = System.currentTimeMillis();
        response.addProperty("writeRedisTimeMs", afterWriteRedisTime - beforeWriteRedisTime);
    }

    private JsonObject getK8sConfig() {
        String gkeConfig = "GKE_" + GoogleUtils.getRegionName();
        return RestTemplateConfig.k8sConfigMap.get(gkeConfig);
    }

    private String getGoogleToken() {
        try {
            String keyPath = ModuleConfig.K8S_KEY_JSON_PATH;
            GoogleCredentials credentials = GoogleCredentials.fromStream(Files.newInputStream(Paths.get(keyPath)))
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));;
            return credentials.refreshAccessToken().getTokenValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getK8sUrl() {
        JsonObject data = getK8sConfig();
        return data.get("k8s_server_path").getAsString();
    }

    private JsonArray getGameServerJsonArr() {
        String k8sServer = getK8sUrl();
        HttpEntity<String> entity = getHttpEntity();
        String response = restTemplate.exchange(k8sServer + "/apis/agones.dev/v1/gameservers", HttpMethod.GET, entity, String.class).getBody();
        return JsonParser.parseString(response).getAsJsonObject().get("items").getAsJsonArray();
    }

    private HttpEntity<String> getHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        String token = getGoogleToken();
        headers.set("Authorization", "Bearer "+ token);
        return new HttpEntity<>(headers);
    }

    private JsonObject getGameServerById(String id) {
        String k8sServer = getK8sUrl();
        HttpEntity<String> entity = getHttpEntity();
        String endpoint = String.format("%s/%s", "/apis/agones.dev/v1/namespaces/default/gameservers", id);
        String response = restTemplate.exchange(k8sServer + endpoint, HttpMethod.GET, entity, String.class).getBody();
        return JsonParser.parseString(response).getAsJsonObject();
    }

    private List<GameServerDO> convertGameServerToDo() {
        JsonArray gamesServerArr = getGameServerJsonArr();
        List<GameServerDO> gameServers = new ArrayList<>();
        for (JsonElement item : gamesServerArr) {
            JsonObject gameServerData = item.getAsJsonObject();
            JsonObject gameServerStatus = gameServerData.get("status").getAsJsonObject();
            JsonObject gameServerSpec = gameServerData.get("spec").getAsJsonObject();
            JsonObject metadata = gameServerData.get("metadata").getAsJsonObject();
            String addr = gameServerStatus.get("address").getAsString();
            int port = gameServerStatus.get("ports").getAsJsonArray().get(0).getAsJsonObject().get("port").getAsInt();
            String gameServerName = gameServerSpec.get("container").getAsString();
            String gameServerId = metadata.get("name").getAsString();
            //get label version -> for logic allocation game server
            JsonObject labels = metadata.get("labels").getAsJsonObject();
            String version = "";
            if (labels.has("version")) {
                version = labels.get("version").getAsString();
            }
            int capacity;
            int count;
            //for PlayerTracking
            JsonElement players = gameServerStatus.get("players");
            if (!players.isJsonNull()) {
                capacity = gameServerStatus.get("players").getAsJsonObject().get("capacity").getAsInt();
                count = gameServerStatus.get("players").getAsJsonObject().get("count").getAsInt();

                //for CountsAndLists
            } else {
                capacity = gameServerStatus.get("counters").getAsJsonObject().get("rooms").getAsJsonObject().get("capacity").getAsInt();
                count = gameServerStatus.get("counters").getAsJsonObject().get("rooms").getAsJsonObject().get("count").getAsInt();
            }
            String state = gameServerStatus.get("state").getAsString();
            setGameServerData(addr, port, capacity, count, state, gameServerName, gameServerId, gameServers, version);
        }
        return gameServers;
    }

    public void getGameServer(TicketEnt ticketEnt) {
        /*
        logic select game server:
        1) match with user version
        2) match with "Allocated" status
        3) availableRoom < BUFFER_ROOM
        4) min availableRoom
         */
        List<GameServerDO> gameServers = convertGameServerToDo();
        Optional<GameServerDO> optional = gameServers.stream()
                .filter(gs -> gs.getGameServerName().equals(ticketEnt.getGameServerNameSpace()))
                .filter(gs -> gs.getState().equals("Allocated"))
                .filter(gs -> gs.getAvailableRoom() > BUFFER_ROOM)
                .min(Comparator.comparing(GameServerDO::getAvailableRoom));
        if (optional.isPresent()) {
            GameServerDO gameServerDO = optional.get();
            if (gameServerDO.getAddress() != null) {
                ticketEnt.setIp(gameServerDO.getAddress());
            }
            ticketEnt.setPort(gameServerDO.getPort());
            ticketEnt.setCurrentRoom(gameServerDO.getCurrentRom());
            ticketEnt.setMaxRoom(gameServerDO.getMaxRoom());
            ticketEnt.setGameServerId(gameServerDO.getGameServerId());
        }
    }

    /*
    logic to allocation 1 game server:
    Check the current allocated game server first, if (capacity - count) < MIN_AVAILABLE_ROOM_TO_ALLOCATE => allocate new "Ready" game server
     */
    public void allocationGameServer(String key, String value) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("apiVersion", "allocation.agones.dev/v1");
        requestBody.put("kind", "GameServerAllocation");

        Map<String, Object> spec = new HashMap<>();
        Map<String, Object> selector1 = new HashMap<>();
        Map<String, Object> selector2 = new HashMap<>();

        Map<String, String> matchLabels1 = new HashMap<>();
        matchLabels1.put(key, value);
        selector1.put("matchLabels", matchLabels1);
        selector1.put("gameServerState", "Allocated");

        Map<String, Object> counters = new HashMap<>();
        Map<String, Integer> rooms = new HashMap<>();
        rooms.put("minAvailable", MIN_AVAILABLE_ROOM_TO_ALLOCATE);
        counters.put("rooms", rooms);
        selector1.put("counters", counters);

        Map<String, String> matchLabels2 = new HashMap<>();
        matchLabels2.put(key, value);
        selector2.put("matchLabels", matchLabels2);
        selector2.put("gameServerState", "Ready");

        spec.put("selectors", new Object[]{selector1, selector2});
        requestBody.put("spec", spec);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String token = getGoogleToken();
            headers.set("Authorization", "Bearer "+ token);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            String k8sServer = getK8sUrl();
            String url = k8sServer + "/apis/allocation.agones.dev/v1/namespaces/default/gameserverallocations";
            restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        } catch (Exception e) {
            String log = String.format("An error occur when allocate game server: %s", e.getLocalizedMessage());
            GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.EXCEPTION, TicketService.class));
        }
    }

    public void updateReadyStatus(String id) {
        if (!id.isEmpty()) {
            List<GameServerDO> gameServerDOS = convertGameServerToDo();
            String gameServerNameSpace = gameServerDOS.stream().filter(gs -> gs.getGameServerId().contentEquals(id)).findFirst().orElse(new GameServerDO()).getGameServerName();
            if (gameServerNameSpace != null) {
                int count = (int) gameServerDOS.stream()
                        .filter(gs -> gs.getGameServerName().contentEquals(gameServerNameSpace))
                        .filter(gs -> gs.getState().equals("Allocated"))
                        .count();
                if (count > 1) {
                    JsonObject gameServerJson = getGameServerById(id);
                    JsonObject gameServerStatus = gameServerJson.get("status").getAsJsonObject();
                    gameServerStatus.addProperty("state", "Ready");
                    updateGameServer(id, gameServerJson);
                }
            }
        } else {
            if (ModuleConfig.IS_DEBUG) {
                String log = String.format("Time: %s Agones update status fail cause game server id is null", DateTimeUtils.getNow("hh:MM:ss"));
                GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, TicketService.class));
            }
        }
    }

    private void setGameServerData(String addr, int port, int capacity, int count, String state,
                                   String gameServerName, String gameServerId, List<GameServerDO> gameServers, String version) {
        GameServerDO gameServerDO = new GameServerDO();
        gameServerDO.setAddress(addr);
        gameServerDO.setPort(port);
        gameServerDO.setMaxRoom(capacity);
        gameServerDO.setCurrentRom(count);
        gameServerDO.setState(state);
        gameServerDO.setGameServerName(gameServerName);
        gameServerDO.setGameServerId(gameServerId);
        int available = capacity - count;
        if (available > 0) {
            gameServerDO.setAvailableRoom(available);
        } else {
            gameServerDO.setAvailableRoom(-1);
        }
        gameServers.add(gameServerDO);
    }

    public void setDataUpdateRoomCount(int count, String id) {
        if (!id.isEmpty()) {
            JsonObject gameServerJson = getGameServerById(id);
            JsonObject gameServerStatus = gameServerJson.get("status").getAsJsonObject();
            JsonObject room = gameServerStatus.get("counters").getAsJsonObject().get("rooms").getAsJsonObject();
            int maxRoom = room.get("capacity").getAsInt();
            room.addProperty("count", count);
            updateRoomCount(id, gameServerJson, count, maxRoom);
        } else {
            if (ModuleConfig.IS_DEBUG) {
                String log = String.format("Time: %s Agones update room fail cause game server id is null", DateTimeUtils.getNow("hh:MM:ss"));
                GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, TicketService.class));
            }
        }
    }
    private void updateGameServer(String id, JsonObject body) {
        try {
            String k8sServer = getK8sUrl();
            String uri = String.format("/apis/agones.dev/v1/namespaces/default/gameservers/%s", id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String token = getGoogleToken();
            headers.set("Authorization", "Bearer "+ token);
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            restTemplate.exchange(k8sServer + uri, HttpMethod.PUT, entity, String.class);
        }catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(e.getLocalizedMessage(), TeleLogType.EXCEPTION, TicketService.class));
        }

    }

    private void updateRoomCount(String id, JsonObject body, int count, int maxRoom) {
        try{
            updateGameServer(id, body);
            if (ModuleConfig.IS_DEBUG) {
                String log = String.format("Time: %s Agones update room: current room %d, max room %d", DateTimeUtils.getNow("hh:MM:ss"), count, maxRoom);
                GMLocalQueue.addQueue(new TelegramLoggerCmd(log, TeleLogType.DEBUG, TicketService.class));
            }
        }catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(e.getLocalizedMessage(), TeleLogType.EXCEPTION, TicketService.class));
        }
    }
}
