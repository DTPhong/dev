/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.service.event;

import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.db.match3.EventWriteRepository;
import cc.bliss.match3.service.gamemanager.db.match3.TriggerWriteRepository;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.TriggerEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.ETriggerStatus;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.EventEnt;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Phong
 */
@Service
public class TriggerService extends BaseService {

    private static final Map<Integer, TriggerEnt> mapTrigger;
    private static final Map<Integer, EventEnt> mapEvent;
    private static long lastRefreshTime;
    // 5 phut
    private static long refreshTime = 30l * 60l * 1000l;

    static {
        if (ModuleConfig.IS_TEST) {
            refreshTime = 5000;
        }
        mapTrigger = new NonBlockingHashMap<>();
        mapEvent = new NonBlockingHashMap<>();
        lastRefreshTime = 0;
    }

    private static void clearCache() {
        mapTrigger.clear();
        mapEvent.clear();
    }

    private void checkRefreshCache() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime < refreshTime) {
            return;
        }
        adminRefreshCache();
    }

    public void adminRefreshCache() {
        long now = System.currentTimeMillis();
        clearCache();
        List<TriggerEnt> activeTrigger = triggerRepository.read().getCurrentListTrigger(
                ETriggerStatus.ENABLE.getValue());
        activeTrigger.forEach(e -> {
            mapTrigger.put(e.getId(), e);
        });
        eventRepository.read().findAll().forEach(e -> {
            mapEvent.put(e.getId(), e);
        });
        lastRefreshTime = now;
    }

    public TriggerEnt getCurrentSingleTrigger(int type) {
        checkRefreshCache();
        for (Map.Entry<Integer, TriggerEnt> entry : mapTrigger.entrySet()) {
            if ((entry.getValue().getType() == type) && checkAvailableTime(entry.getValue())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public List<TriggerEnt> getCurrentListTrigger(int type) {
        checkRefreshCache();
        List<TriggerEnt> activeTrigger = new ArrayList<>();
        mapTrigger.entrySet().forEach(e -> {
            if ((e.getValue().getType() == type) && checkAvailableTime(e.getValue())) {
                activeTrigger.add(e.getValue());
            }
        });
        return activeTrigger;
    }

    private boolean checkAvailableTime(TriggerEnt triggerEnt) {
        // check time, repeate
        if (triggerEnt.getStartTime().getTime() > System.currentTimeMillis() || triggerEnt.getEndTime().getTime() < System.currentTimeMillis()) {
            return false;
        }
        JsonObject object = JSONUtil.DeSerialize(triggerEnt.getTriggerTime(), JsonObject.class);

        JsonArray active_times = object.get("active_times").getAsJsonArray();
        if (active_times.size() <= 0) {
            return false;
        }
        Calendar cl = ModuleConfig.CALENDAR_INSTANCE;
        cl.setTime(new Date());

        int timeInMins = cl.get(Calendar.HOUR_OF_DAY) * 60 + cl.get(Calendar.MINUTE);
        boolean available = false;
        for (int i = 0; i < active_times.size(); i++) {
            JsonArray minutes = active_times.get(i).getAsJsonArray();
            if (minutes.get(0).getAsInt() <= timeInMins && timeInMins <= minutes.get(1).getAsInt()) {
                available = true;
                break;
            }
        }

        if (available == false) {
            return false;
        }

        JsonArray active_days = object.get("active_days").getAsJsonArray();
        JsonArray repeat_days_in_week = object.get("repeat_days_in_week").getAsJsonArray();
        JsonArray repeat_days_in_month = object.get("repeat_days_in_month").getAsJsonArray();

        if (active_days.size() <= 0 && repeat_days_in_week.size() <= 0 && repeat_days_in_month.size() <= 0) {
            return false;
        }

        int dayOfMonth = cl.get(Calendar.DAY_OF_MONTH);
        if (active_days.size() > 0) {
            for (int i = 0; i < active_days.size(); i++) {
                if (dayOfMonth == active_days.get(i).getAsInt()) {
                    return true;
                }
            }
        }

        if (repeat_days_in_month.size() > 0) {
            for (int i = 0; i < repeat_days_in_month.size(); i++) {
                if (dayOfMonth == repeat_days_in_month.get(i).getAsInt()) {
                    return true;
                }
            }
        }

        int dayOfWeek = cl.get(Calendar.DAY_OF_WEEK);

        if (repeat_days_in_week.size() > 0) {
            for (int i = 0; i < repeat_days_in_week.size(); i++) {
                if (dayOfWeek == repeat_days_in_week.get(i).getAsInt()) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<EventEnt> getListEvent(List<Integer> ids) {
        List<EventEnt> list = new ArrayList<>();
        for (Integer id : ids) {
            EventEnt eventEnt = getEvent(id);
            if (eventEnt == null) {
                continue;
            }
            list.add(eventEnt);
        }
        return list;
    }

    public TriggerEnt getTriggerByEventID(int id) {
        return mapTrigger.values().stream().filter(e -> e.getRefId() == id).findFirst().orElse(null);
    }

    public EventEnt getEvent(int eventId) {
        return mapEvent.get(eventId);
    }
}
