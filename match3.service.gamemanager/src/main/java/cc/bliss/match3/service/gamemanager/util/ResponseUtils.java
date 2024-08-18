/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.util;

import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

/**
 * @author Phong
 */
public class ResponseUtils {

    public static <E> JsonObject toSearchResponseData(Page<E> data, NetWorkAPI netWorkAPI) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("data", ModuleConfig.GSON_BUILDER.toJsonTree(data.getContent()));
        jsonObject.addProperty("total_count", data.getTotalElements());
        jsonObject.addProperty("total_page", data.getTotalPages());
        jsonObject.addProperty("limit", data.getNumberOfElements());
        jsonObject.addProperty("current_page", data.getNumber());
        jsonObject.addProperty("api", netWorkAPI.getValue());
        return jsonObject;
    }

    public static JsonObject toSearchResponseData(JsonArray data, long totalCount, int totalPage, int currentPage, int limit, NetWorkAPI netWorkAPI) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("data", data);
        jsonObject.addProperty("total_count", totalCount);
        jsonObject.addProperty("total_page", totalPage);
        jsonObject.addProperty("limit", limit);
        jsonObject.addProperty("current_page", currentPage);
        jsonObject.addProperty("api", netWorkAPI.getValue());
        return jsonObject;
    }

    public static JsonObject toEmptySearchResponseData(NetWorkAPI netWorkAPI) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("data", new JsonArray());
        jsonObject.addProperty("total_count", 0);
        jsonObject.addProperty("total_page", 0);
        jsonObject.addProperty("limit", ModuleConfig.DATA_LIMIT);
        jsonObject.addProperty("current_page", 0);
        jsonObject.addProperty("api", netWorkAPI.getValue());
        return jsonObject;
    }

    public static String toResponseBody(int status, JsonObject data, NetWorkAPI netWorkAPI) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", status);
        jsonObject.add("data", data);
        jsonObject.addProperty("api", netWorkAPI.getValue());
        return jsonObject.toString();
    }

    public static JsonObject toWinBattleProgressResponseBody(int status, JsonObject data) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", status);
        jsonObject.add("data", data);
        jsonObject.addProperty("api", NetWorkAPI.UNKNOWN.getValue());
        return jsonObject;

    }

    public static JsonObject toResponseBody(int status, JsonObject data, JsonArray reward, int chestInfo) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", status);
        jsonObject.add("data", data);
        JsonObject rewardJson = new JsonObject();
        rewardJson.add("rewardArr", reward);
        rewardJson.addProperty("chestInfo", chestInfo);
        jsonObject.add("reward", rewardJson);
        jsonObject.addProperty("api", NetWorkAPI.UNKNOWN.getValue());
        return jsonObject;
    }

    public static JsonObject toResponseBody(int status, JsonObject data, JsonArray reward, int chestInfo, JsonArray currency) {
        JsonObject jsonObject = toResponseBody(status, data, reward, chestInfo);
        jsonObject.add("currency", currency);
        return jsonObject;
    }

    public static JsonObject toResponseBody(int status, JsonObject data, JsonArray currency, NetWorkAPI netWorkAPI) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", status);
        jsonObject.add("data", data);
        jsonObject.addProperty("api", netWorkAPI.getValue());
        jsonObject.add("currency", currency);
        return jsonObject;
    }

    public static String toResponseBody(int status, JsonObject data, JsonArray reward) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", status);
        jsonObject.add("data", data);
        jsonObject.add("reward", reward);
        jsonObject.addProperty("api", NetWorkAPI.UNKNOWN.getValue());
        return jsonObject.toString();
    }

    public static String toResponseBody(int status, double data, NetWorkAPI netWorkAPI) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", status);
        jsonObject.addProperty("data", data);
        jsonObject.addProperty("api", netWorkAPI.getValue());
        return jsonObject.toString();
    }

    public static String toResponseBody(int status, long data, NetWorkAPI netWorkAPI) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", status);
        jsonObject.addProperty("data", data);
        jsonObject.addProperty("api", netWorkAPI.getValue());
        return jsonObject.toString();
    }

    public static String toResponseBody(int status, JsonArray data, NetWorkAPI netWorkAPI) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", status);
        jsonObject.add("data", data);
        jsonObject.addProperty("api", netWorkAPI.getValue());
        return jsonObject.toString();
    }

    public static String toResponseBody(int status, String data, NetWorkAPI netWorkAPI) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", status);
        jsonObject.addProperty("data", data);
        jsonObject.addProperty("api", netWorkAPI.getValue());
        return jsonObject.toString();
    }

    public static String toErrorBody(String error, NetWorkAPI netWorkAPI) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        jsonObject.addProperty("data", error);
        jsonObject.addProperty("api", netWorkAPI.getValue());
        return jsonObject.toString();
    }

    public static String toErrorBody(int error, NetWorkAPI netWorkAPI) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        jsonObject.addProperty("data", error);
        jsonObject.addProperty("api", netWorkAPI.getValue());
        return jsonObject.toString();
    }

}
