package cc.bliss.match3.service.gamemanager.ent.enums;

import org.apache.commons.lang3.StringUtils;

public enum StatusCode {
    success(0, "Success."), // Android success | Apple success | Card success
    unknown_error(-1, "Unknown error, try again."),
    incorrect_product_id(-2, "Incorrect product id."),
    partner_id_exist(-3, "Partner id exist."),
    appstore_error(-4, "Appstore error."),
    pending(-5, "pending"),
    // Apple status code
    status_code_21000(21000, "The request to the App Store was not made using the HTTP POST request method."),
    status_code_21001(21001, "This status code is no longer sent by the App Store."),
    status_code_21002(21002, "The data in the receipt-data property was malformed or the service experienced a temporary issue. Try again."),
    status_code_21003(21003, "The receipt could not be authenticated."),
    status_code_21004(21004, "The shared secret you provided does not match the shared secret on file for your account."),
    status_code_21005(21005, "The receipt server was temporarily unable to provide the receipt. Try again."),
    status_code_21006(21006, "This receipt is valid but the subscription has expired. When this status code is returned to your server, the receipt data is also decoded and returned as part of the response. Only returned for iOS 6-style transaction receipts for auto-renewable subscriptions."),
    status_code_21007(21007, "This receipt is from the test environment, but it was sent to the production environment for verification."),
    status_code_21008(21008, "This receipt is from the production environment, but it was sent to the test environment for verification."),
    status_code_21009(21009, "Internal data access error. Try again later."),
    status_code_21010(21010, "The user account cannot be found or has been deleted."),
    status_code_21100_21199(2110021199, "Internal data access error."),
    // Card status code
    invalid_param(1, "Invalid param"),
    invalid_sign(2, "Invalid sign"),
    card_invalid(16, "Card invalid"),
    cheat(-6,"cheat"),
    system_error(30, "System error");

    public static String translateStatusCode(String error) {
        StatusCode[] codes = StatusCode.values();
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].getStringValue().equalsIgnoreCase(error)) {
                return codes[i].toString();
            }
        }
        return StringUtils.EMPTY;
    }

    public static StatusCode getStatusCode(int intValue) {
        StatusCode[] codes = StatusCode.values();
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].getIntValue() == intValue) {
                return codes[i];
            }
        }
        return StatusCode.unknown_error;
    }

    public static StatusCode getStatusCode(String strValue) {
        StatusCode[] codes = StatusCode.values();
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].getStringValue().equalsIgnoreCase(strValue)) {
                return codes[i];
            }
        }
        return StatusCode.unknown_error;
    }

    private int intValue;
    private String description;

    private StatusCode(int intValue, String description) {
        this.intValue = intValue;
        this.description = description;
    }

    public int getIntValue() {
        return this.intValue;
    }

    public String getStringValue() {
        return this.intValue + StringUtils.EMPTY;
    }

    public String getDescription() {
        return this.description;
    }
}
