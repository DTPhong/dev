package cc.bliss.match3.service.gamemanager.service.shop;

import bliss.lib.framework.common.LogUtil;
import bliss.lib.framework.redis.RedisFactory;
import bliss.lib.framework.util.JSONUtil;
import bliss.lib.framework.util.NetworkUtils;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.ent.common.VerifyResult;
import cc.bliss.match3.service.gamemanager.ent.enums.StatusCode;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import com.vdurmont.emoji.EmojiParser;
import ga.log4j.GA;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class GoogleProcessor {

    private static final String TAG = GoogleProcessor.class.getName();

    private final static String GET_TOKEN_GOOGLE = "https://accounts.google.com/o/oauth2/token";

    private final static String URL_CHECK_PAYMENT_GOOGLE_V3 = "https://www.googleapis.com/androidpublisher/v3/applications";
    private final static String CARDPAY_QUERY_TEMPLATE_V3 = "/%s/purchases/products/%s/tokens/%s?access_token=%s";
    private final static String FIELD_ACCESS_TOKEN = "payment_gg_token1";
    private final static int TOKEN_EXPIRE_MILIS = 25 * 60; // 25 mins

    private static final String URL_CHECK_SUB = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/%s/purchases/subscriptionsv2/tokens/%s?key=%s";
    private static final String packageName = "com.bliss.kplay.gamebai";

    private static final String apiKey = "AIzaSyAp2CjxYfe5ODq9kv01TNteOHZxuzw13No";

    @Autowired
    @Qualifier("redisTemplateString")
    protected RedisTemplate<String, String> redisTemplateString;

    public VerifyResult processPaymentV3(String receipt) {
        VerifyResult result = new VerifyResult();
        result.setStatusCode(StatusCode.unknown_error);
        result.setPurchaseTimeMillis(0);
        String strResultPayment = StringUtils.EMPTY;
        try {
            ReceiptInfo purchaseInfo = JSONUtil.DeSerialize(receipt, ReceiptInfo.class);
            result.setProductKey(purchaseInfo.productId);

            System.out.println("Receipt Data: " + receipt);

            String accessToken = null;
            if (accessToken == null || accessToken.isEmpty()) {
                String strResult = getAccessToken(purchaseInfo.packageName);
                AccessToken access_token = JSONUtil.DeSerialize(strResult, AccessToken.class);
                if (access_token != null) {
                    accessToken = access_token.access_token;
                }
            }

            System.out.println("AccessToken: " + accessToken);

            String url = URL_CHECK_PAYMENT_GOOGLE_V3 + String.format(CARDPAY_QUERY_TEMPLATE_V3, purchaseInfo.packageName, purchaseInfo.productId, purchaseInfo.purchaseToken, accessToken);
            url = url.replaceAll(" ", "%20");
            System.out.println("URL VERIFY >> " + url);
            if (accessToken != null) {
                strResultPayment = NetworkUtils.getResponse(url);
                if(StringUtils.isNotBlank(strResultPayment)){
                    System.out.println("Result: \n" + strResultPayment);

                    InAppPurchaseResultInfoV3 resultInfo = JSONUtil.DeSerialize(strResultPayment, InAppPurchaseResultInfoV3.class);
                    if (resultInfo != null) {
                        result.setPartnerId(resultInfo.orderId);
                        switch(resultInfo.getPurchaseState()){
                            case 0:
                                result.setStatusCode(StatusCode.success);
                                result.setPurchaseTimeMillis(resultInfo.purchaseTimeMillis);
                                break;
                            case 2:
                                result.setStatusCode(StatusCode.pending);
                                break;
                            default:
                                result.setStatusCode(StatusCode.unknown_error);
                                break;
                        }

                        result.setPurchaseType(resultInfo.purchaseType);
                    }

                    System.out.println("Result local info: \n" + JSONUtil.Serialize(result));
                }else{
                    String teleMsg = "Verify IAP error: " + url;
                    teleMsg = EmojiParser.parseToUnicode(teleMsg);
                    GMLocalQueue.addQueue(new TelegramLoggerCmd(teleMsg, TeleLogType.EXCEPTION, GoogleProcessor.class));
                }
            } else {
                result.setStatusCode(StatusCode.unknown_error);
            }
        } catch (Exception ex) {
            result.setStatusCode(StatusCode.appstore_error);
            System.out.println(TAG + LogUtil.stackTrace(ex));
            System.out.println("Error with data receipt: " + receipt);
            System.out.println("Error with data result : " + strResultPayment);
        }
        return result;
    }

    private void saveAccessToken(String accessToken) {
        redisTemplateString.opsForValue().set(FIELD_ACCESS_TOKEN, accessToken);
        redisTemplateString.expire(FIELD_ACCESS_TOKEN, TOKEN_EXPIRE_MILIS, TimeUnit.MILLISECONDS);
    }

    private String getCacheAccessToken() {
        return redisTemplateString.opsForValue().get(FIELD_ACCESS_TOKEN);
    }

    public String getAccessToken(String packageName) {
        String access_token = getCacheAccessToken();

        if (StringUtils.isNotBlank(access_token)) {
            return access_token;
        }

        String refresh_token = ModuleConfig.ANDROID_REFRESH_TOKEN;
        String client_id = ModuleConfig.ANDROID_CLIENT_ID;
        String client_secret = ModuleConfig.ANDROID_CLIENT_SECRET;

        System.out.println("package >> " + packageName);
        System.out.println("config >> " + refresh_token + " ---- " + client_id + " --- " + client_secret);

        if (refresh_token == null || refresh_token.isEmpty() || client_id == null || client_id.isEmpty() || client_secret == null || client_secret.isEmpty()) {
            return StringUtils.EMPTY;
        }

        String url = GET_TOKEN_GOOGLE;

        try {
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            String urlParameters = "grant_type=refresh_token" + "&"
                    + "refresh_token=" + refresh_token + "&"
                    + "client_id=" + client_id + "&"
                    + "client_secret=" + client_secret;

            System.out.println("param >> " + urlParameters);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = rd.readLine()) != null) {
                response.append(inputLine);
            }
            rd.close();

            access_token = response.toString();
            saveAccessToken(access_token);
        } catch (Exception ex) {
            access_token = StringUtils.EMPTY;
            System.out.println(ex);
        }
        return access_token;
    }

    public static String get(String urlRequest, Map<String, String> header) throws Exception {
        String result = "";
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlRequest);
            conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            Set<String> keys = header.keySet();
            for (String key : keys) {
                conn.setRequestProperty(key, header.get(key));
            }

            conn.setUseCaches(false);
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                while ((line = reader.readLine()) != null) {
                    result += "\r\n" + line;
                }
            }
            if (result.length() > 0) {
                result = result.substring(2);
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return result;
    }
}


class AccessToken {
    public String access_token;
    public String token_type;
    public String expires_in;
}

class ReceiptInfo
{
    public String orderId;
    public String packageName;
    public String productId;
    public long purchaseTime;
    public int purchaseState;
    public String purchaseToken;
}
@Data
class InAppPurchaseResultInfoV3
{
    public String kind;
    public long purchaseTimeMillis;
    public int purchaseState;
    public int consumptionState;
    public String developerPayload;
    public String orderId;
    public int acknowledgementState;
    public int purchaseType = 1;
}
class PaymentConfig
{
    public String app_package_id;
    public int sandbox;
    public String android_refresh_token;
    public String android_client_id;
    public String android_client_secret;
    public String ios_client_secret;
    public String cardpay_url_new;
    public String cardpay_public_url;
    public String cardpay_clientid;
    public String cardpay_secretkey;
    public String cardpay_provider;
}