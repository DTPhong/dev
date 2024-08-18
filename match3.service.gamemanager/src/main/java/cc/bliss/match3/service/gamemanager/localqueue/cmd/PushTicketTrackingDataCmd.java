package cc.bliss.match3.service.gamemanager.localqueue.cmd;

import bliss.lib.framework.queue.QueueCommand;
import cc.bliss.match3.service.gamemanager.ent.common.Statistic;
import cc.bliss.match3.service.gamemanager.ent.common.TicketEnt;
import cc.bliss.match3.service.gamemanager.ent.data.MatchingTicketEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.HeroEnt;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.service.common.HeroService;
import cc.bliss.match3.service.gamemanager.service.common.ProfileStatisticService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;

public class PushTicketTrackingDataCmd implements QueueCommand {

    ProfileService profileService;
    HeroService heroService;
    ProfileStatisticService profileStatisticService;
    TicketEnt currentTicket;

    int roomID;
    int botType;
    TicketEnt opponentTicket;
    EPushTicketTrackingData pushTicketTrackingData;
    Producer producerService;

    /**
     * For cancel case
     */
    public PushTicketTrackingDataCmd(ProfileService profileService, HeroService heroService, ProfileStatisticService profileStatisticService, TicketEnt currentTicket, EPushTicketTrackingData pushTicketTrackingData, Producer producerService) {
        this.profileService = profileService;
        this.heroService = heroService;
        this.profileStatisticService = profileStatisticService;
        this.currentTicket = currentTicket;
        this.pushTicketTrackingData = pushTicketTrackingData;
        this.producerService = producerService;
    }

    /**
     * For matched case
     */
    public PushTicketTrackingDataCmd(Producer producerService, EPushTicketTrackingData pushTicketTrackingData, TicketEnt opponentTicket, int botType, int roomID, TicketEnt currentTicket, ProfileStatisticService profileStatisticService, HeroService heroService, ProfileService profileService) {
        this.producerService = producerService;
        this.pushTicketTrackingData = pushTicketTrackingData;
        this.opponentTicket = opponentTicket;
        this.botType = botType;
        this.roomID = roomID;
        this.currentTicket = currentTicket;
        this.profileStatisticService = profileStatisticService;
        this.heroService = heroService;
        this.profileService = profileService;
    }

    @Override
    public void execute() {
        try {
            switch (pushTicketTrackingData){
                case CANCEL:
                {
                    Profile profile = profileService.getProfileByID(currentTicket.getUserID());
                    HeroEnt heroEnt = heroService.getHero(currentTicket.getUserID(), profile.getSelectHero());
                    Statistic statistic = profileStatisticService.getStatistic(profile.getId(), profile.getSelectHero());
                    MatchingTicketEnt matchingTicketEnt = MatchingTicketEnt.buildCancelTicket(profile, currentTicket, heroEnt, statistic);
                    producerService.sendMatchingTicketMessage(matchingTicketEnt);
                    break;
                }
                case MATCHED:{
                    Profile profile = profileService.getProfileByID(currentTicket.getUserID());
                    HeroEnt heroEnt = heroService.getHero(currentTicket.getUserID(), profile.getSelectHero());
                    Statistic statistic = profileStatisticService.getStatistic(profile.getId(), profile.getSelectHero());
                    MatchingTicketEnt matchingTicketEnt = MatchingTicketEnt.buildMatchedTicket(profile, currentTicket, heroEnt, statistic, roomID, botType, opponentTicket);
                    producerService.sendMatchingTicketMessage(matchingTicketEnt);
                    break;
                }
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }
}

