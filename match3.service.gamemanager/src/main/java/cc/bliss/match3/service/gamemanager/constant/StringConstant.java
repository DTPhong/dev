/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.constant;

/**
 * @author Phong
 */
public class StringConstant {

    public static String COMMON_NOT_ENOUGH_MONEY = "Not enough money !!!";
    public static String TELE_ADD_AGENCY = "[Config Agency] %s Thêm UserID: %s (%s).\nBắt đầu: %s - Kết thúc: %s";
    public static String TELE_REMOVE_AGENCY = "[Config Agency] %s Xoá UserID: %s (%s).";
    public static String TELE_ADD_SEND_MONEY = "[Config Send-NCoin User] %s Thêm UserID: %s (%s).\nBắt đầu: %s - Kết thúc: %s. TAX: %s";
    public static String TELE_REMOVE_SEND_MONEY = "[Config Send-NCoin User] %s Xóa UserID: %s (%s).";
    public static String TELE_ADD_SPECIAL_ATM = "[Config Special-atm User] %s Thêm UserID: %s (%s).\nBắt đầu: %s - Kết thúc: %s. TAX: %s";
    public static String TELE_REMOVE_SPECIAL_ATM = "[Config Special-atm User] %s Xóa UserID: %s (%s).";

    public static String TELE_CONFIG_VIP = "[Config VIP Point] %s %s UserID: %s (%s). VIP Point: %s, VIP Before: %s, VIP After: %s, Reason: %s";
    public static String TELE_TRANFER_MONEY = "[Transfer Money] %s %s UserID: %s (%s). NCoin: %s. Reason: %s";
    public static String TELE_TRANFER_BCOIN = "[Transfer Bcoin] %s %s UserID: %s (%s). BCoin: %s. Reason: %s";
    public static String TELE_TRANFER_KCOIN = "[Transfer Kcoin] %s %s UserID: %s (%s). KCoin: %s. Reason: %s";
    public static String TELE_LOCK_USER = "[Lock User] Khoá tài khoản UserID: %s (%s). VIP: %s. Reason: %s";

    public static String MAIL_REFUNC_AVATAR_TITLE = "Hoàn trả NCoin của lỗi Mua Ảnh Đại Diện";
    public static String MAIL_REFUNC_AVATAR_CONTENT = "Thân gửi người chơi,\n"
            + "\n"
            + "Chúng tôi rất xin lỗi về Lỗi trừ tiền nhiều lần khi mua Ảnh Đại Diện trong thời gian vừa qua.\n"
            + "Sau khi kiểm tra, chúng tôi hoàn lại toàn bộ số NCoin bị tính nhầm lại cho bạn.\n"
            + "Ngoài ra, KPlay xin gửi tặng bạn phần quà nhỏ nhằm đền bù cho lỗi này ở thư sau.\n"
            + "\n"
            + "Cảm ơn bạn.";

    public static String MAIL_BONUS_AVATAR_TITLE = "Quà đền bù Lỗi Mua Ảnh Đại Diện";
    public static String MAIL_BONUS_AVATAR_CONTENT = "Thân gửi người chơi,\n"
            + "\n"
            + "KPlay thân tặng bạn phần quà nhỏ trị giá 500K NCoin nhằm đền bù cho lỗi trừ tiền nhiều lần khi mua Ảnh Đại Diện trong thời gian vừa qua.\n"
            + "\n"
            + "Chúng tôi xin lỗi cho sự bất tiện này.\n"
            + "\n"
            + "Chúc bạn chơi game vui vẻ.";

    public static String MAIL_SEND_MONEY_ACTIVE_TITLE = "Thông báo mở tính năng";
    public static String MAIL_SEND_MONEY_ACTIVE_CONTENT = "Chúc mừng bạn đã đạt đủ điều kiện để sử dụng Tính năng TẶNG NCOIN!\n"
            + "- Thời gian hiệu lực: đến hết ngày %s\n"
            + "- Điều kiện duy trì: từ ngày %s đến ngày %s, TỔNG NẠP TRONG NGÀY >= 100K VND bạn sẽ có thể tiếp tục sử dụng tính năng này đến hết ngày %s và tiếp tục tương tự như vậy cho những tháng tiếp theo.\n"
            + "- Cách sử dụng: Ở màn hình Chính của game > bấm vào Ảnh Đại Diện của bạn > sẽ thấy nút Tặng Ncoin > Nhập ID Tài khoản của người Nhận để Tặng.\n"
            + "Mọi thắc mắc, bạn có thể liên hệ Hotline 0909.616.353\n"
            + "Cảm ơn bạn đã ủng hộ KPlay. Chúc bạn chơi game vui vẻ!";

    public static String MAIL_SEND_MONEY_NOTI_TITLE = "Tính năng sắp hết hạn!";
    public static String MAIL_SEND_MONEY_NOTI_CONTENT = "THÔNG BÁO\n"
            + "\n"
            + "Tính năng TẶNG NCOIN của bạn sẽ hết hạn sau ngày %s!\n"
            + "Điều Kiện Duy Trì:\n"
            + "- Từ ngày %s đến ngày %s, bạn chỉ cần thoả điều kiện TỔNG NẠP TRONG NGÀY >= 100K VND là sẽ có thể tiếp tục sử dụng tính năng này đến hết ngày %s nhé.\n"
            + "Mọi thắc mắc, bạn có thể liên hệ Hotline 0909.616.353\n"
            + "Cảm ơn bạn đã ủng hộ KPlay. Chúc bạn chơi game vui vẻ!";

    public static String MAIL_SEND_MONEY_EXPIRE_TITLE = "Tính năng đã hết hạn!";
    public static String MAIL_SEND_MONEY_EXPIRE_CONTENT = "THÔNG BÁO\n"
            + "\n"
            + "Tính năng TẶNG NCOIN của bạn đã hết hạn và bạn không thoả điều kiện để tiếp tục sử dụng!\n"
            + "Điều kiện để MỞ LẠI TÍNH NĂNG: bạn chỉ cần thoả điều kiện TỔNG NẠP TRONG NGÀY >= 100K VND là sẽ có\n"
            + "thể sử dụng lại tính năng TẶNG NCOIN nhé.\n"
            + "Mọi thắc mắc, bạn có thể liên hệ Hotline 0909.616.353\n"
            + "Chúc bạn chơi game vui vẻ!";
}
