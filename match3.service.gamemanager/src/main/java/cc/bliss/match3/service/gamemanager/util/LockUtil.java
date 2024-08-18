package cc.bliss.match3.service.gamemanager.util;

import cc.bliss.match3.service.gamemanager.constant.NetWorkAPI;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class LockUtil {

    private final RedissonClient redissonClient;

    @Autowired
    public LockUtil(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    private String getLockKey(long userId, NetWorkAPI netWorkAPI){
        return String.format("distributed_lock_%d_%s", userId, netWorkAPI.getValue());
    }

    /**
     * Acquire a distributed lock with an expiration time.
     * @param key the lock key
     * @param timeout the timeout in milliseconds
     * @return true if the lock was acquired, false otherwise
     */
    public boolean acquireLock(long userId, NetWorkAPI netWorkAPI) {
        RLock lock = redissonClient.getLock(getLockKey(userId, netWorkAPI));
        try {
            // Attempt to acquire the lock with a specified timeout
            return lock.tryLock(0, 5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Release a distributed lock.
     * @param key the lock key
     */
    public void releaseLock(long userId, NetWorkAPI netWorkAPI) {
        RLock lock = redissonClient.getLock(getLockKey(userId, netWorkAPI));
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
