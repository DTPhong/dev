package cc.bliss.match3.service.gamemanager.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

public class UserActivityUtil {
    private static final long USER_EXPIRE_MINUTES = 20;
    private static final long SSE_EXPIRE_MINUTES = 30;
    private static final long SSE_EXPIRE_MILLISECONDS = SSE_EXPIRE_MINUTES * 60 * 1000;
    private static UserActivityUtil instance;
    private final Cache<Long, Long> lastActivityCache;

    private UserActivityUtil() {
        this.lastActivityCache = CacheBuilder.newBuilder()
                .expireAfterWrite(USER_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    public static synchronized UserActivityUtil getInstance() {
        if (instance == null) {
            instance = new UserActivityUtil();
        }
        return instance;
    }

    public void updateLastActivity(long userId) {
        lastActivityCache.put(userId, System.currentTimeMillis());
    }

    public boolean isInactive(long userId) {
        Long lastActivityTime = lastActivityCache.getIfPresent(userId);
        if (lastActivityTime == null) {
            return true;
        }
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastActivityTime) >= SSE_EXPIRE_MILLISECONDS;
    }

    public Cache<Long, Long> lastActivityCache() {
        return lastActivityCache;
    }
}
