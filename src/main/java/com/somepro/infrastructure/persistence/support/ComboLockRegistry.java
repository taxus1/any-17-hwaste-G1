package com.somepro.infrastructure.persistence.support;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 组合键互斥锁注册表（基础设施层）。
 *
 * 单实例部署下，用 JVM 级分段锁把「同一单位+类别」的库存变动与盘点状态流转串行化：
 * - 入库 / 联单转出：锁内做盘点冻结检查再动账；
 * - 盘点单状态流转：锁内做前置状态条件更新；
 * - 立单：按「单位+类别+月份」锁内查重，保证并发立单只成一份。
 *
 * 锁不释放内存（key 基数是单位×类别，量级可控），换来实现简单与无超时死等。
 */
@Component
public class ComboLockRegistry {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /** 库存互斥 key：同一单位+类别的入库、转出、盘点开工共用一把锁。 */
    public String inventoryKey(Long sourceId, String categoryCode) {
        return "hwaste:inv:" + sourceId + ":" + categoryCode;
    }

    /** 立单查重 key：同一单位+类别+月份只准落一张未作废盘点单。 */
    public String checkCreateKey(Long sourceId, String categoryCode, String checkPeriod) {
        return "hwaste:check:" + sourceId + ":" + categoryCode + ":" + checkPeriod;
    }

    /** 在 key 对应的锁内执行动作（阻塞式，调用方需已在 boundedElastic 线程上）。 */
    public <T> T withLock(String key, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
