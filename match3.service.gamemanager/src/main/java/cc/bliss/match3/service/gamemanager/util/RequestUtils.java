/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.util;

import bliss.lib.framework.util.JSONUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * @author Phong
 */
public class RequestUtils {

    public static JsonObject requestToJson(HttpServletRequest request) {
        JsonObject jsonObject = new JsonObject();
        try {
            String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            return JSONUtil.DeSerialize(body, JsonObject.class);
        } catch (IOException ex) {
        }
        return jsonObject;
    }

    public static JsonArray requestToArr(HttpServletRequest request) {
        JsonArray jsonObject = new JsonArray();
        try {
            String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            return JSONUtil.DeSerialize(body, JsonArray.class);
        } catch (IOException ex) {
        }
        return jsonObject;
    }

}
