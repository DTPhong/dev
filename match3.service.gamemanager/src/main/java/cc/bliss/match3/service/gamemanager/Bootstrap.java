package cc.bliss.match3.service.gamemanager;

import bliss.lib.framework.common.LogUtil;
import cc.bliss.match3.service.gamemanager.config.ModuleConfig;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.MatchingTicketCmd;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.service.BaseService;
import cc.bliss.match3.service.gamemanager.service.common.HeroService;
import cc.bliss.match3.service.gamemanager.service.event.DailyQuestService;
import cc.bliss.match3.service.gamemanager.service.event.GachaService;
import cc.bliss.match3.service.gamemanager.service.event.TriggerService;
import cc.bliss.match3.service.gamemanager.service.system.ProfileService;
import cc.bliss.match3.service.gamemanager.service.system.RoomService;
import cc.bliss.match3.service.gamemanager.service.system.TicketService;
import cc.bliss.match3.service.gamemanager.util.GoogleUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = "cc.bliss.match3", exclude = RedisRepositoriesAutoConfiguration.class)
@EntityScan(basePackages = "cc.bliss.match3.service.gamemanager.ent")
public class Bootstrap {

    public static void main(String[] args) {
        try {

            ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder(Bootstrap.class)
                    .properties("spring.config.location:" + ModuleConfig.SPRING_BOOT_CONFIG)
                    .build().run(args);
            TicketService ticketService = applicationContext.getBean(TicketService.class);
            RoomService roomService = applicationContext.getBean(RoomService.class);
            TriggerService triggerService = applicationContext.getBean(TriggerService.class);
            DailyQuestService dailyQuestService = applicationContext.getBean(DailyQuestService.class);
            GachaService gachaService = applicationContext.getBean(GachaService.class);
            ProfileService profileService = applicationContext.getBean(ProfileService.class);
            HeroService heroService = applicationContext.getBean(HeroService.class);
            dailyQuestService.initQuestPool();
            triggerService.adminRefreshCache();
            gachaService.refreshConfig();
            GMLocalQueue.addQueue(new MatchingTicketCmd(ticketService, roomService, profileService, heroService));
            GMLocalQueue.addQueue(new TelegramLoggerCmd("===========> start success", TeleLogType.EXCEPTION, Bootstrap.class));

        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, Bootstrap.class));
        }
    }

}
