/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cc.bliss.match3.service.gamemanager.localqueue.cmd;

import bliss.lib.framework.queue.QueueCommand;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.ent.common.TicketEnt;
import cc.bliss.match3.service.gamemanager.ent.enums.EHeroRarity;
import cc.bliss.match3.service.gamemanager.ent.enums.TicketStatus;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.service.common.HeroService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.service.system.RoomService;
import cc.bliss.match3.service.gamemanager.service.system.TicketService;
import cc.bliss.match3.service.gamemanager.util.RandomItem;
import cc.bliss.match3.service.gamemanager.util.RandomUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Phong
 */
public class MatchingTicketCmd implements QueueCommand {

    TicketService ticketService;
    RoomService roomService;
    ProfileService profileService;
    HeroService heroService;

    public MatchingTicketCmd(TicketService ticketService, RoomService roomService, ProfileService profileService, HeroService heroService) {
        this.ticketService = ticketService;
        this.roomService = roomService;
        this.profileService = profileService;
        this.heroService = heroService;
    }

    /**
     * @param deltaTime
     * Xác suất gặp Bot như sau: 5s: 30%, 6s: 50%, 7s: 70%, 8s: 90%, 9s: 100%
     * Close test config: 7s: 30%, 8s: 50%, 9s: 70%, 10s: 90%, 11s: 100%
     */
    private boolean isMatchingBot(long deltaTime){
        List<RandomItem> itemList = new ArrayList<>();
        if (deltaTime < Duration.ofSeconds(7).toMillis()){
            return false;
        } else if (deltaTime < Duration.ofSeconds(8).toMillis()){
            itemList.add(RandomItem.builder().data(1).percent(30).build());
            itemList.add(RandomItem.builder().data(0).percent(70).build());
            return RandomUtils.random(itemList) == 1;
        }else if (deltaTime < Duration.ofSeconds(9).toMillis()){
            itemList.add(RandomItem.builder().data(1).percent(50).build());
            itemList.add(RandomItem.builder().data(0).percent(50).build());
            return RandomUtils.random(itemList) == 1;
        }else if (deltaTime < Duration.ofSeconds(10).toMillis()){
            itemList.add(RandomItem.builder().data(1).percent(70).build());
            itemList.add(RandomItem.builder().data(0).percent(30).build());
            return RandomUtils.random(itemList) == 1;
        }else if (deltaTime < Duration.ofSeconds(11).toMillis()){
            itemList.add(RandomItem.builder().data(1).percent(90).build());
            itemList.add(RandomItem.builder().data(0).percent(10).build());
            return RandomUtils.random(itemList) == 1;
        } else {
            return true;
        }
    }

    @Override
    public void execute() {
        System.out.println("Starting ticket processing...");
        while (!TicketService.IS_MAINTAIN) {
            List<TicketEnt> list = new ArrayList<>();
            list.addAll(TicketService.mapTicket.values());
            for (int i = 0; i < list.size(); i++) {
                try{
                    TicketEnt cur = list.get(i);
                    if (cur.isExpiredTicket()){
                        ticketService.delTicket(cur.getUserID());
                        list.remove(i);
                        i--;
                        continue;
                    }
                    if (cur.getStatus().equals(TicketStatus.ON_ROOM) || cur.getStatus().equals(TicketStatus.MATCHED)) {
                        continue;
                    }
                    // delay match ticket 0.3s
                    if (cur.getDeltaTime() < Duration.ofSeconds(1).toMillis()){
                        continue;
                    }
                    // case tutorial
                    if (cur.getBattleWon() == 0){
                        // trainer 1 - aaron lv1
                        cur.setBotID(1);
                        Profile botProfile = profileService.getProfileByID(1);
                        ticketService.matchTicket(cur, botProfile);
                        break;
                    } else if (cur.getBattleWon() == 1){
                        // trainer 2 - ivy lv1
                        cur.setBotID(2);
                        Profile botProfile = profileService.getProfileByID(2);
                        ticketService.matchTicket(cur, botProfile);
                        break;
                    } else if (cur.getBattleWon() == 2){
                        // trainer 3 - nataluna lv1
                        cur.setBotID(3);
                        Profile botProfile = profileService.getProfileByID(3);
                        ticketService.matchTicket(cur, botProfile);
                        break;
                    } else if (cur.getBattleWon() == 3){
                        // victoria lv1
                        cur.setBotID(4);
                        Profile botProfile = profileService.getProfileByID(4);
                        ticketService.matchTicket(cur, botProfile);
                        break;
                    } else if (cur.getBattleWon() == 4 && cur.getLoseStreak() == 0){
                        // noah lv3
                        cur.setBotID(7);
                        Profile botProfile = profileService.getProfileByID(7);
                        ticketService.matchTicket(cur, botProfile);
                        break;
                    }
                    // waiting too long
                    boolean isMatchingBot = isMatchingBot(cur.getDeltaTime());
                    if (isMatchingBot){

                        // User đợi quá lâu, tìm con bot phù hợp cho user
                        int minBotHard = cur.getMinBotHardLevel();
                        int maxBotHard = cur.getMaxBotHardLevel();
                        int botHard = RandomUtils.random(minBotHard, Math.max(minBotHard+1, maxBotHard));


                        int minBotHeroLv = cur.getMinBotHeroLevel();
                        int maxBotHeroLv = cur.getMaxBotHeroLevel();
                        int botHeroLv = RandomUtils.random(minBotHeroLv, Math.max(minBotHeroLv+1, maxBotHeroLv));

                        long botID = RandomUtils.random(8, 31);
                        cur.setBotID(botID);
                        Profile profileBot = profileService.getProfileByID(botID);

                        // nếu botHero < 3 thì ko thể chọn Bot có hero Epic trở lên (chỉ chọn tướng có hero Mythic)
                        if(botHeroLv < 3){
                            List<Integer> epicHeroIds = heroService.getHeroIdsByRarity(EHeroRarity.MYTHIC);
                            List<Profile> botProfiles = profileService.getBotProfileByHeroSelectedIn(epicHeroIds);
                            int profileIndex = RandomUtils.random(0, Math.max(botProfiles.size() - 1, 1));
                            profileBot = botProfiles.get(profileIndex);
                        }
                        System.out.println(cur.getUserID() + " match vs bot " + profileBot.getDisplayName());
                        System.out.println("botHard: "+ botHard+ ", botHeroLv: "+ botHeroLv);
                        System.out.println("------");
                        profileBot.setBotType(botHard);
                        ticketService.cacheMatchingTicket(cur.getUserID(), botHeroLv);
                        ticketService.matchTicket(cur, profileBot);
                        break;
                    } else {
                        // Loop qua các ticket tìm ticket phù hợp
                        for (int j = 0; j < list.size(); j++) {
                            TicketEnt opponent = list.get(j);
                            if (opponent.getStatus().equals(TicketStatus.ON_ROOM) || opponent.getStatus().equals(TicketStatus.MATCHED)) {
                                continue;
                            }
                            if (opponent.getUserID() == cur.getUserID()) {
                                continue;
                            }
                            if (opponent.getRoomID() != cur.getRoomID()) {
                                continue;
                            }
                            if (!opponent.getGameServerNameSpace().contentEquals(cur.getGameServerNameSpace())) {
                                continue;
                            }
                            if (profileService.isFTUE(opponent.getBattleWon(), opponent.getLoseStreak())){
                                continue;
                            }
                            /**
                             * Match với người, điều kiện phù hợp:
                             * 1) Cùng nhóm winStreak
                             * 2) Cùng nhóm heroTrophy
                             * 3) Chênh lệch heroLevel gần nhau
                             * 4) Chênh lệch heroTrophy gần nhau
                             */
                            if (opponent.getRoomID() == 0 && cur.getRoomID() == 0) {
                                cur.getComparingTicket().put(opponent.getUserID(), opponent.getInitTime());

                                // Đk 1: User phải cùng group winStreak
                                if(cur.getGroupWinStreak() != opponent.getGroupWinStreak()){
                                    continue;
                                }

                                // Đk 2: user phải cùng group heroTrophy
                                if(cur.getGroupHeroTrophy() != opponent.getGroupHeroTrophy()){
                                    continue;
                                }

                                // Đk 3: 2 user chênh lệch heroLevel trong khoảng cho phép
                                if(!(cur.isOpponentHeroLevelInRange(opponent.getLevel())
                                        && opponent.isOpponentHeroLevelInRange(cur.getLevel()))
                                ){
                                    continue;
                                }

                                // Đk 4: 2 user Chênh lệch heroTrophy trong khoảng cho phép
                                if(!(cur.isOpponentHeroTrophyInRange(opponent.getHeroTrophy())
                                        || opponent.isOpponentHeroTrophyInRange(cur.getHeroTrophy()))
                                ){
                                    continue;
                                }
                            }
                            ticketService.matchTicket(cur, opponent);
                            break;
                        }
                    }
                } catch(Exception ex){
                    System.out.println(ex.getMessage());
                }
            }
        }
    }

}
