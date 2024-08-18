/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.util;

import bliss.lib.framework.common.LogUtil;
import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.JSONUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * @author phong
 */
public class GoogleUtils {

    static String CLIENT_ID = "365245680869-oht7i605ljrqp8tihufjf9ogsn96g30c.apps.googleusercontent.com";
    static GoogleIdTokenVerifier googleIdTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
            .setAudience(Collections.singletonList(CLIENT_ID))
            .build();

    public static JsonObject fetchGoogleAccount2(String googleToken) {
        try {
            GoogleIdToken googleIdToken = googleIdTokenVerifier.verify(googleToken);
            if (googleIdToken != null) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("email", ConvertUtils.toString(googleIdToken.getPayload().getEmail()));
                jsonObject.addProperty("id", ConvertUtils.toString(googleIdToken.getPayload().getSubject()));
                jsonObject.addProperty("name", ConvertUtils.toString(googleIdToken.getPayload().getUnknownKeys().get("name")));
                jsonObject.addProperty("picture", ConvertUtils.toString(googleIdToken.getPayload().getUnknownKeys().get("picture")));
                return jsonObject;
            }
        } catch (Exception ex) {
            System.out.println(LogUtil.stackTrace(ex));
        }
        return null;
    }

    public static JsonObject fetchGoogleAccount(String googleToken) {
        try {
            String rest = String.format("/oauth2/v1/userinfo?access_token=%s", new Object[]{googleToken});

            String rs = HttpUtils.get("https://www.googleapis.com", rest);

            return JSONUtil.DeSerialize(rs, JsonObject.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public static boolean isGoogleAvatar(String avatarPath) {
        return avatarPath.contains("googleusercontent");
    }

    public static String getRegionName() {
        try{
            String metadataUrl = "http://metadata.google.internal/computeMetadata/v1/instance/zone";
            HttpHeaders headers = new HttpHeaders();
            headers.add("Metadata-Flavor", "Google");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(metadataUrl, HttpMethod.GET, entity, String.class);
            String zone = response.getBody();

            if (zone != null && zone.contains("/")) {
                // Extract the region from the zone, e.g., projects/123456789012/zones/asia-southeast1-b
                String[] parts = zone.split("/");
                String zoneName = parts[parts.length - 1];
                String[] zoneParts = zoneName.split("-");
                if (zoneParts.length > 0) {
                    return zoneParts[0].toUpperCase();
                }
            }

        }catch (Exception e) {
            return "ASIA";
        }
        return "ASIA";
    }
}
