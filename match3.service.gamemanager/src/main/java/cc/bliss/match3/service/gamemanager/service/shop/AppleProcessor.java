package cc.bliss.match3.service.gamemanager.service.shop;

import bliss.lib.framework.common.LogUtil;
import bliss.lib.framework.util.JSONUtil;
import bliss.lib.framework.util.NetworkUtils;
import cc.bliss.match3.service.gamemanager.ent.common.VerifyResult;
import cc.bliss.match3.service.gamemanager.ent.enums.StatusCode;
import cc.bliss.match3.service.gamemanager.util.GameUtils;
import com.google.gson.JsonObject;
import ga.log4j.GA;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

public class AppleProcessor {

    private static final String TAG = AppleProcessor.class.getName();
    private final static String URL_PRODUCTION = "https://buy.itunes.apple.com/verifyReceipt";
    private final static String URL_SANDBOX = "https://sandbox.itunes.apple.com/verifyReceipt";

    public static VerifyResult processPayment(String receipt, long invoiceId, boolean isTest) {
        VerifyResult result = new VerifyResult();
        result.setStatusCode(StatusCode.unknown_error);
        String strResultPayment = StringUtils.EMPTY;
        try {
            String url = URL_PRODUCTION;
            result.setPurchaseType(1);
            if (isTest) {
                url = URL_SANDBOX;
                result.setPurchaseType(0);
            }

            JsonObject requestParams = new JsonObject();
            requestParams.addProperty("receipt-data", receipt);
            strResultPayment = NetworkUtils.doPost(url, new HashMap<>(), requestParams);
            GA.app.error("Result: " + strResultPayment);

            if (strResultPayment != null) {
                JsonObject resultInfo = JSONUtil.DeSerialize(strResultPayment, JsonObject.class);

                if (resultInfo.get("status").getAsInt() == 0) {

                    JsonObject receiptInfo = resultInfo.getAsJsonObject("receipt");

                    String packageName = receiptInfo.get("bid").getAsString();
                    if(!GameUtils.isValidIosPackage(packageName)){
                        result.setStatusCode(StatusCode.cheat);
                        return result;
                    }

                    result.setProductKey(receiptInfo.get("product_id").getAsString());
                    result.setPartnerId(receiptInfo.get("transaction_id").getAsString());
                    result.setStatusCode(StatusCode.success);
                    GA.app.error("SET STATUS SUCCESS ===== " + strResultPayment);
                }

                if (resultInfo.get("status").getAsInt() == 21007) {
                    result.setStatusCode(StatusCode.status_code_21007);
                    GA.app.error("SET STATUS SANDBOX ===== " + strResultPayment);

                }
            }

        } catch (Exception ex) {
            result.setStatusCode(StatusCode.appstore_error);
            GA.app.error(TAG + LogUtil.stackTrace(ex));
            GA.app.error("Error with data receipt: " + receipt);
            GA.app.error("Error with data result : " + strResultPayment);
            GA.app.error("Error with data invoiceId : " + invoiceId);
        }
        return result;
    }

}
