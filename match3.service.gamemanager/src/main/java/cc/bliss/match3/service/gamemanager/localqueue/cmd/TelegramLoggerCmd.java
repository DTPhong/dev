/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.localqueue.cmd;

import bliss.lib.framework.common.Config;
import bliss.lib.framework.queue.QueueCommand;
import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.HttpHelper;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;

import java.net.URLEncoder;

/**
 * @author baotn
 */
public class TelegramLoggerCmd implements QueueCommand {

    private static final String TOKEN = ConvertUtils.toString(Config.getParam("log_tele", "user_token"));
    private static final String CHAT_ID = ConvertUtils.toString(Config.getParam("log_tele", "user_id"));
    private static final int EXCEPTION_THREAD_ID = ConvertUtils.toInt(Config.getParam("log_tele", "exception_thread"));
    private static final int DEBUG_THREAD_ID = ConvertUtils.toInt(Config.getParam("log_tele", "debug_thread"));
    private static final String DATE_TIME_FORMAT = "dd MMM yyyy HH:mm:ss";
    private static String DEBUG_URL = String.format("https://api.telegram.org/bot%s/", TOKEN);
    private String data;
    private TeleLogType teleLogType;
    private Class clazz;
    private int topicID;

    public TelegramLoggerCmd(String data, TeleLogType teleLogType, Class clazz) {
        this.data = data;
        this.teleLogType = teleLogType;
        this.clazz = clazz;
    }

    public TelegramLoggerCmd(String data, TeleLogType teleLogType, Class clazz, int topicID) {
        this.data = data;
        this.teleLogType = teleLogType;
        this.clazz = clazz;
        this.topicID = topicID;
    }

    @Override
    public void execute() {
        try {
            int threadId;
            switch (teleLogType) {
                case EXCEPTION:
                    threadId = EXCEPTION_THREAD_ID;
                    break;
                case DEBUG:
                    threadId = DEBUG_THREAD_ID;
                    break;
                default:
                    threadId = EXCEPTION_THREAD_ID;
            }
            int maxLen = Math.min(data.length(), 2990);
            data += "\n" + clazz.getName();
            long strLen = data.length();
            if (strLen > 2990) {
                for (int i = 0; i <= Math.round(strLen / maxLen); i++) {
                    int length = (i + 1) * maxLen > data.length() ? data.length() : (i + 1) * maxLen;
                    String dataRequest = URLEncoder.encode(data.substring(i * maxLen, length), "UTF-8");
                    String queries = getQuery(CHAT_ID, threadId, dataRequest);
                    HttpHelper.post(DEBUG_URL, queries);
                }
            } else {
                String dataRequest = URLEncoder.encode(data.substring(0, maxLen), "UTF-8");
                String queries = getQuery(CHAT_ID, threadId, dataRequest);
                HttpHelper.post(DEBUG_URL, queries);
            }
        } catch (Exception ex) {
            System.out.println("QueueTelegramLogger send telegram log fail msg: " + data);
            System.out.println("QueueTelegramLogger send telegram log fail : " + ex.toString());
        }
    }

    private String getQuery(String chatID, int threadId, String msg) {
        String queries = String.format("sendMessage?chat_id=%s&message_thread_id=%s&text=%s", chatID, threadId, msg);
        return queries;
    }
}
