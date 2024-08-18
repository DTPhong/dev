package cc.bliss.match3.service.gamemanager.service;

import bliss.lib.framework.common.LogUtil;
import cc.bliss.match3.service.gamemanager.db.*;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.Profile;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.rabbitmq.Producer;
import cc.bliss.match3.service.gamemanager.service.event.EventService;
import cc.bliss.match3.service.gamemanager.service.system.GameLogService;
import cc.bliss.match3.service.gamemanager.service.system.SSEService;
import cc.bliss.match3.service.gamemanager.util.LockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.persistence.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public abstract class BaseService {
    @Autowired
    protected DeviceMappingRepository deviceMappingRepository;
    @Autowired
    protected ProfileRepository profileRepository;
    @Autowired
    protected ClanRepository clanRepository;
    @Autowired
    protected ClanMemberRepository clanMemberRepository;
    @Autowired
    protected ConfigRepository configRepository;
    @Autowired
    protected EventService eventService;
    @Autowired
    protected FriendRepository friendRepository;
    @Autowired
    protected HeroRepository heroRepository;
    @Autowired
    protected InventoryRepository inventoryRepository;
    @Autowired
    protected InvoiceRepository invoiceRepository;
    @Autowired
    protected MailRepository mailRepository;
    @Autowired
    protected ProductRepository productRepository;
    @Autowired
    protected ProfileStatisticRepository profileStatisticRepository;
    @Autowired
    protected QuestPoolRepository questPoolRepository;
    @Autowired
    protected RequestDeleteAccountRepository requestDeleteAccountRepository;
    @Autowired
    protected TriggerRepository triggerRepository;
    @Autowired
    protected TrophyRoadRepository trophyRoadRepository;
    @Autowired
    protected EventRepository eventRepository;
    @Autowired
    protected VersionRepository versionRepository;
    @Autowired
    protected SSEService sseService;
    @Autowired
    protected Producer producer;
    @Autowired
    protected LockUtil lockUtil;
    @Autowired
    @Qualifier("redisTemplateString")
    protected RedisTemplate<String, String> redisTemplateString;
    @Autowired
    @Qualifier("redisTemplate")
    protected RedisTemplate<Object, Object> redisTemplate;
    @Autowired
    protected HashOperations<String, String, String> hashOperations;
    @Autowired
    protected RedisConnectionFactory redisConnection;
    @PersistenceUnit(unitName = "logEntityManagerFactory")
    protected EntityManagerFactory logEntityManagerFactory;
    @PersistenceUnit(unitName = "match3EntityManagerFactory")
    protected EntityManagerFactory match3EntityManagerFactory;

    protected static final String DATE_TIME_FORMAT = "HH:mm:ss-dd/MM/yyyy";
    protected static final String DATE_FORMAT = "dd/MM/yyyy";
    protected static final String DATE_WEEK_FORMAT = "ww/MM/yyyy";
    protected static final String DATE_MONTH_FORMAT = "MM/yyyy";

    public <T> T insertMatch3SchemaData(T entity) {
        EntityTransaction transaction = null;
        EntityManager em = null;
        try {
            em = match3EntityManagerFactory.createEntityManager();
            transaction = em.getTransaction();

            transaction.begin();
            em.persist(entity);
            em.flush();
            transaction.commit();
        } catch (Exception ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(ex), TeleLogType.EXCEPTION, GameLogService.class));
        } finally {
            if (em != null){
                em.close();
            }
            return entity;
        }
    }

    public <T> List<T> insertMatch3SchemaListData(List<T> entity) {
        EntityTransaction transaction = null;
        EntityManager em = null;
        try {
            em = match3EntityManagerFactory.createEntityManager();
            transaction = em.getTransaction();

            transaction.begin();
            entity.forEach(em::persist);
            em.flush();
            transaction.commit();
        } catch (Exception ex) {
            if (transaction != null) {
                transaction.rollback();
            }
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(ex), TeleLogType.EXCEPTION, GameLogService.class));
        } finally {
            if (em != null){
                em.close();
            }
            return entity;
        }
    }
}
