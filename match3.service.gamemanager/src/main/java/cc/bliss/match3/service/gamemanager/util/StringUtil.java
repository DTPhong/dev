/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;

/**
 * @author Phong
 */
public class StringUtil {

    static final NumberFormat DECIMAL_PATTERN = new DecimalFormat("###,###,###");
    private static final Random RND = new Random(System.currentTimeMillis());

    public static String formatDecimal(long value) {
        return DECIMAL_PATTERN.format(value).replace(",", ".");
    }

    public static boolean isNullOrEmpty(String str) {
        if (str == null) {
            return true;
        }
        return str.isEmpty();
    }

    public static boolean isNotNullOrEmpty(String str) {
        if (str == null) {
            return false;
        }
        return !str.isEmpty();
    }
}
