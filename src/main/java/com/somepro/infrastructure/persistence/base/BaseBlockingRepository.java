package com.somepro.infrastructure.persistence.base;

import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 阻塞 JDBC → 响应式链路的桥接基类（基础设施层）。
 *
 * 与 DemoItemRepositoryImpl 里的私有 blocking(...) 同一约定，抽成基类供各仓储适配器复用：
 * 1. 先在响应式线程上从 Reactor Context 取操作人（切线程后就取不到了）；
 * 2. 再切到 boundedElastic 执行阻塞 JDBC（绝不能在 Netty event-loop 上跑 JDBC）；
 * 3. 操作人放进 AuditContextHolder，供 MetaObjectHandler 填充 createBy / updateBy。
 */
public abstract class BaseBlockingRepository {

    protected <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
