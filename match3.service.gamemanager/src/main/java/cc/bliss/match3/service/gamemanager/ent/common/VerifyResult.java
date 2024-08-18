package cc.bliss.match3.service.gamemanager.ent.common;

import cc.bliss.match3.service.gamemanager.ent.enums.StatusCode;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;

@ToString
public class VerifyResult {

    private String productKey;
    private String partnerId;
    private StatusCode statusCode;
    private String partnerResult;
    private int purchaseType; // for google test
    private int cashBackBCoin;
    private long purchaseTimeMillis;

    /*
        The type of purchase of the inapp product. This field is only set if this purchase was not made using the standard in-app billing flow. Possible values are:
        0. Test (i.e. purchased from a license testing account)
        1. Promo (i.e. purchased using a promo code)
        2. Rewarded (i.e. from watching a video ad instead of paying)
     */
    public VerifyResult() {
        this.productKey = StringUtils.EMPTY;
        this.partnerId = StringUtils.EMPTY;
        this.statusCode = StatusCode.unknown_error;
        this.partnerResult = StringUtils.EMPTY;
        this.purchaseType = 1; // not google test
        this.cashBackBCoin = 0;
    }

    public int getCashBackBCoin() {
        return cashBackBCoin;
    }

    public void setCashBackBCoin(int cashBackBCoin) {
        this.cashBackBCoin = cashBackBCoin;
    }

    public String getProductKey() {
        return productKey;
    }

    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerResult() {
        return partnerResult;
    }

    public void setPartnerResult(String partnerId) {
        this.partnerResult = partnerId;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public int getPurchaseType() {
        return purchaseType;
    }

    public void setPurchaseType(int purchaseType) {
        this.purchaseType = purchaseType;
    }

    public long getPurchaseTimeMillis() {
        return purchaseTimeMillis;
    }

    public void setPurchaseTimeMillis(long purchaseTimeMillis) {
        this.purchaseTimeMillis = purchaseTimeMillis;
    }
}
