/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.common;

import bliss.lib.framework.util.ConvertUtils;
import bliss.lib.framework.util.DateTimeUtils;
import bliss.lib.framework.util.JSONUtil;
import bliss.lib.framework.util.StringUtils;
import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import cc.bliss.match3.service.gamemanager.db.match3.MailWriteRepository;
import cc.bliss.match3.service.gamemanager.ent.common.MailReward;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.common.UpdateMoneyResult;
import cc.bliss.match3.service.gamemanager.ent.enums.*;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.MailEnt;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.system.AdminService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.util.RequestUtils;
import cc.bliss.match3.service.gamemanager.util.ResponseUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author Phong
 */
@Service
public class MailService extends BaseService {

    @Autowired
    AdminService adminService;

    @Autowired
    ProfileService profileService;

    public String getByID(int id) {
        Optional<MailEnt> optional = mailRepository.read().findById(id);
        if (optional.isPresent()) {
            MailEnt mailEnt = optional.get();
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), buildMailJson(mailEnt), NetWorkAPI.GET_CONFIG);
        }
        return ResponseUtils.toErrorBody("Invalid parameters", NetWorkAPI.PROFILE_BY_ID);
    }

    private List<MailEnt> getCurrentListMail(long userId) {
        return mailRepository.read().getCurrentListMail(userId, EMailStatus.DELETE.getValue(), new Timestamp(System.currentTimeMillis()));
    }

    public String getCurrentListMail() {
        SessionObj sessionObj = adminService.getSession();
        long userID = sessionObj.getId();
        List<MailEnt> listMail = getCurrentListMail(userID);
        if (isReadAllMail(userID)) {
            setReddot(userID);
        }
        JsonArray jsonArray = buildListMailJson(listMail);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), jsonArray, NetWorkAPI.GET_CONFIG);
    }

    public String addMail(HttpServletRequest request) {
        JsonObject requestJson = RequestUtils.requestToJson(request);
        MailEnt mailEnt = new MailEnt();
        mailEnt.setContent(requestJson.get("content").getAsString());
        mailEnt.setStatus(requestJson.get("status").getAsInt());
        mailEnt.setExpiredTime(new Date(requestJson.get("expiredTime").getAsLong()));
        mailEnt.setReceivedTime(new Date(requestJson.get("receivedTime").getAsLong()));
        mailEnt.setTitle(requestJson.get("title").getAsString());
        mailEnt.setUserId(requestJson.get("userID").getAsInt());
        mailEnt.setRewards(requestJson.get("rewards").getAsJsonArray().toString());
        insertMatch3SchemaData(mailEnt);
        return ResponseUtils.toResponseBody(HttpStatus.OK.value(), buildMailJson(mailEnt), NetWorkAPI.GET_CONFIG);
    }

    public String readMail(HttpServletRequest request) {
        SessionObj sessionObj = adminService.getSession();
        JsonObject requestJson = RequestUtils.requestToJson(request);
        long userID = sessionObj.getId();

        int id = requestJson.get("id").getAsInt();
        if (id > 0) {
            MailEnt mail = mailRepository.read().getById(id);
            mail.setStatus(EMailStatus.READ.getValue());
            mailRepository.write().updateStatusById(id, EMailStatus.READ.getValue());

            List<MailEnt> listMail = new ArrayList<>();
            listMail.add(mail);
            if (isReadAllMail(userID)) {
                pushReddotNoti(userID, ReddotConstant.MAIL.getValue(), false);
            }
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), buildMailJson(mail), NetWorkAPI.GET_CONFIG);
        } else {
            List<MailEnt> listMail = getCurrentListMail(userID);
            // remove cac mail khac mail thong bao
            listMail.removeIf(e -> getMailType(e) != EMailType.NOTICE);
            for (int i = 0; i < listMail.size(); i++) {
                MailEnt mail = listMail.get(i);

                if (mail.getStatus() == EMailStatus.UNREAD.getValue()) {
                    mail.setStatus(EMailStatus.READ.getValue());
                    mailRepository.write().updateStatusById(id, EMailStatus.READ.getValue());
                }
            }
            pushReddotNoti(userID, ReddotConstant.MAIL.getValue(), false);

            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), buildListMailJson(listMail), NetWorkAPI.GET_CONFIG);
        }
    }

    public String deleteMail(HttpServletRequest request) {
        SessionObj sessionObj = adminService.getSession();
        JsonObject requestJson = RequestUtils.requestToJson(request);
        long userID = sessionObj.getId();

        int id = requestJson.get("id").getAsInt();
        if (id > 0) {
            MailEnt mail = mailRepository.read().getById(id);
            if (!org.apache.commons.lang.StringUtils.isEmpty(mail.getRewards())
                    && getMailType(mail) != EMailType.CRITICAL
                    && mail.getStatus() != EMailStatus.CLAIMED.getValue()
                    && mail.getStatus() != EMailStatus.DELETE.getValue()
                    && (mail.getExpiredTime().getTime() - DateTimeUtils.getMilisecondsNow()) >= 0) {
                return null;
            }

            if (mail.getUserId() == -1) {
                // mail all user thì ko xử lý xóa
                return null;
            }

            mail.setStatus(EMailStatus.DELETE.getValue());
            mailRepository.write().updateStatusById(id, EMailStatus.DELETE.getValue());

            List<MailEnt> listMail = new ArrayList<>();
            listMail.add(mail);
            if (isReadAllMail(userID)) {
                pushReddotNoti(userID, ReddotConstant.MAIL.getValue(), false);
            }
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), buildListMailJson(listMail), NetWorkAPI.GET_CONFIG);
        } else {
            List<MailEnt> listMail = getCurrentListMail(userID);
            List<MailEnt> listMailResult = new ArrayList();
            // remove cac mail chua doc
            listMail.removeIf(e -> e.getStatus() == EMailStatus.UNREAD.getValue());
            for (int i = 0; i < listMail.size(); i++) {
                MailEnt mail = listMail.get(i);
                if (getMailType(mail) == EMailType.REWARD && !(mail.getStatus() == EMailStatus.CLAIMED.getValue())) {
                    continue;
                }

                if (getMailType(mail) == EMailType.GIFTCODE && !(mail.getStatus() == EMailStatus.CLAIMED.getValue())) {
                    continue;
                }

                if (getMailType(mail) == EMailType.CARD && !(mail.getStatus() == EMailStatus.CLAIMED.getValue())) {
                    continue;
                }

                if (mail.getUserId() == -1) {
                    // mail all user thì ko xử lý xóa
                    continue;
                }
                mail.setStatus(EMailStatus.DELETE.getValue());
                mailRepository.write().updateStatusById(id, EMailStatus.DELETE.getValue());
                listMailResult.add(mail);
            }
            pushReddotNoti(userID, ReddotConstant.MAIL.getValue(), false);
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), buildListMailJson(listMail), NetWorkAPI.GET_CONFIG);
        }
    }

    public String claimMail(HttpServletRequest request) {
        SessionObj sessionObj = adminService.getSession();
        JsonObject requestJson = RequestUtils.requestToJson(request);
        long userID = sessionObj.getId();
        int id = requestJson.get("id").getAsInt();
        if (id > 0) {
            MailEnt mail = mailRepository.read().getById(id);
            long ncoinReward = 0;
            if (!org.apache.commons.lang.StringUtils.isEmpty(mail.getRewards()) && mail.getStatus() != EMailStatus.CLAIMED.getValue() && mail.getStatus() != EMailStatus.DELETE.getValue() && (mail.getExpiredTime().getTime() - DateTimeUtils.getMilisecondsNow()) >= 0) {
                mail.setStatus(EMailStatus.CLAIMED.getValue());
                mailRepository.write().updateStatusById(id, EMailStatus.CLAIMED.getValue());
                mail = mailRepository.read().getById(id);
                ncoinReward = getGoldReward(mail.getRewards());
                UpdateMoneyResult moneyResult = profileService.updateMoney(userID, ncoinReward, EUpdateMoneyType.MAIL);
                List<MailEnt> listMail = new ArrayList<>();
                listMail.add(mail);

                JsonObject response = buildClaimMailBody(listMail);
                JsonArray updateReward = buildUpdateReward(moneyResult.getDelta(), moneyResult.getAfter());
                return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, updateReward);
            }
        } else {
            List<MailEnt> listMail = getCurrentListMail(userID);
            // remove cac mail khac mail reward
            listMail.removeIf(e -> getMailType(e) != EMailType.REWARD);
            long goldReward = 0;
            long afterMoney = 0;

            for (int i = 0; i < listMail.size(); i++) {
                MailEnt mail = listMail.get(i);
                long ncoinReward = 0;

                if (!org.apache.commons.lang.StringUtils.isEmpty(mail.getRewards()) && mail.getStatus() != EMailStatus.CLAIMED.getValue() && mail.getStatus() != EMailStatus.DELETE.getValue() && (mail.getExpiredTime().getTime() - DateTimeUtils.getMilisecondsNow()) >= 0) {
                    mail.setStatus(EMailStatus.CLAIMED.getValue());
                    mailRepository.write().updateStatusById(id, EMailStatus.CLAIMED.getValue());
                    ncoinReward = getGoldReward(listMail.get(i).getRewards());
                    goldReward += ncoinReward;
                    UpdateMoneyResult moneyResult = profileService.updateMoney(userID, ncoinReward, EUpdateMoneyType.MAIL);
                    afterMoney = moneyResult.getAfter();
                }
            }
            if (isReadAllMail(userID)) {
                pushReddotNoti(userID, ReddotConstant.MAIL.getValue(), false);
            }
            JsonObject response = buildClaimMailBody(listMail);
            JsonArray updateReward = buildUpdateReward(goldReward, afterMoney);
            return ResponseUtils.toResponseBody(HttpStatus.OK.value(), response, updateReward);
        }
        return ResponseUtils.toResponseBody(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.name(), NetWorkAPI.GET_CONFIG);
    }

    private JsonObject buildUpdateMoney(long delta, long after) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("rewardType", ERewardType.GOLD.getValue());
        jsonObject.addProperty("delta", delta);
        jsonObject.addProperty("after", after);
        return jsonObject;
    }

    private JsonObject buildClaimMailBody(List<MailEnt> listMail) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("listMail", buildListMailJson(listMail));
        return jsonObject;
    }

    private JsonArray buildUpdateReward(long deltaMoney, long afterMoney) {
        JsonArray listReward = new JsonArray();
        listReward.add(buildUpdateMoney(deltaMoney, afterMoney));
        return listReward;
    }

    public long getGoldReward(String rewards) {

        List<MailReward> listRewards = getListRewards(rewards);

        long gold = 0;
        for (int i = 0; i < listRewards.size(); i++) {
            if (listRewards.get(i).type == EMailRewardType.MONEY.getValue()) {
                gold += ConvertUtils.toLong(listRewards.get(i).value);
            }
        }

        return gold;
    }

    private boolean isReadAllMail(long userId) {
        return mailRepository.read().getMailCountByStatus(userId, EMailStatus.UNREAD.getValue(), new Timestamp(System.currentTimeMillis())) == 0l;
    }

    private void setReddot(long userID) {
        GMLocalQueue.addQueue(new TelegramLoggerCmd("Set mail reddot not support yet", TeleLogType.EXCEPTION, MailService.class));
    }

    private JsonArray buildListMailJson(List<MailEnt> listMail) {
        JsonArray jsonArray = new JsonArray();
        for (MailEnt mailEnt : listMail) {
            JsonObject jsonObject = buildMailJson(mailEnt);
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    private JsonObject buildMailJson(MailEnt mailEnt) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", mailEnt.getId());
        jsonObject.addProperty("content", mailEnt.getContent());
        jsonObject.addProperty("title", mailEnt.getTitle());
        jsonObject.addProperty("expiredTime", mailEnt.getExpiredTime().getTime());
        jsonObject.addProperty("receivedTime", mailEnt.getReceivedTime().getTime());
        jsonObject.addProperty("status", mailEnt.getStatus());
        jsonObject.add("rewards", buildListRewardJson(mailEnt.getRewards()));

        if (StringUtils.isEmpty(mailEnt.getRewards())) {
            jsonObject.addProperty("type", EMailType.NOTICE.getValue());
        } else {
            try {
                JsonArray jsonArray = JSONUtil.DeSerialize(mailEnt.getRewards(), JsonArray.class);
                JsonObject firstReward = jsonArray.get(0).getAsJsonObject();
                if (firstReward.get("type").getAsString().equals("giftcode")) {
                    firstReward.addProperty("type", EMailType.GIFTCODE.getValue());
                } else if (firstReward.get("type").getAsString().equals("bestfriend")) {
                    firstReward.addProperty("type", EMailType.CARD.getValue());
                } else if (firstReward.get("type").getAsString().equals("critical")) {
                    firstReward.addProperty("type", EMailType.CRITICAL.getValue());
                } else if (firstReward.get("type").getAsString().equals("bcoin")) {
                    firstReward.addProperty("type", EMailType.REWARD.getValue());
                } else {
                    firstReward.addProperty("type", EMailType.REWARD.getValue());
                }
            } catch (Exception e) {
                jsonObject.addProperty("type", EMailType.NOTICE.getValue());
            }
        }
        return jsonObject;
    }

    public JsonArray buildListRewardJson(String rewards) {

        List<MailReward> mailRewards = getListRewards(rewards);
        JsonArray rewardsBuilder = new JsonArray();

        if (rewards != null) {
            for (int i = 0; i < mailRewards.size(); i++) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("type", mailRewards.get(i).type);
                jsonObject.addProperty("value", mailRewards.get(i).value);
                rewardsBuilder.add(jsonObject);
            }
        }

        return rewardsBuilder;
    }

    public List<MailReward> getListRewards(String rewards) {
        List<MailReward> listReward = new ArrayList<>();

        if (org.apache.commons.lang.StringUtils.isEmpty(rewards)) {
            return listReward;
        }

        JsonArray array = JSONUtil.DeSerialize(rewards, JsonArray.class);

        if (array == null || array.size() <= 0) {
            return listReward;
        }

        for (int i = 0; i < array.size(); i++) {
            JsonObject object = array.get(i).getAsJsonObject();

            MailReward reward = new MailReward();

            if (object.get("type").getAsString().equalsIgnoreCase("money")) {
                reward.type = EMailRewardType.MONEY.getValue();
            } else if (object.get("type").getAsString().equalsIgnoreCase("vip_point")) {
                reward.type = EMailRewardType.VIP_POINT.getValue();
            } else if (object.get("type").getAsString().equalsIgnoreCase("giftcode")) {
                reward.type = EMailRewardType.GIFTCODE.getValue();
            } else if (object.get("type").getAsString().equalsIgnoreCase("bcoin")) {
                reward.type = EMailRewardType.BCOIN.getValue();
            } else {
                reward.type = -1;
            }

            reward.value = object.has("value") ? object.get("value").getAsString() : "";

            listReward.add(reward);
        }

        return listReward;
    }

    private void pushReddotNoti(long userID, int value, boolean b) {
        GMLocalQueue.addQueue(new TelegramLoggerCmd("Push mail reddot not support yet", TeleLogType.EXCEPTION, MailService.class));
    }

    public EMailType getMailType(MailEnt mail) {
        if (bliss.lib.framework.util.StringUtils.isEmpty(mail.getRewards())) {
            return EMailType.NOTICE;
        } else {
            JsonArray jsonArray = JSONUtil.DeSerialize(mail.getRewards(), JsonArray.class);
            if (jsonArray.size() == 0) {
                return EMailType.NOTICE;
            }
            JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();
            if (jsonObject.get("type").getAsString().contentEquals("giftcode")) {
                return EMailType.GIFTCODE;
            } else if (jsonObject.get("type").getAsString().contentEquals("bcoin")) {
                return EMailType.REWARD;
            } else if (jsonObject.get("type").getAsString().contentEquals("critical")) {
                return EMailType.CRITICAL;
            } else if (jsonObject.get("type").getAsString().contentEquals("bestfriend")) {
                return EMailType.CARD;
            } else {
                return EMailType.REWARD;
            }
        }
    }
}
