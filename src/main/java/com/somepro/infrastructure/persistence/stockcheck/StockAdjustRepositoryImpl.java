package com.somepro.infrastructure.persistence.stockcheck;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.somepro.domain.stockcheck.model.StockAdjust;
import com.somepro.domain.stockcheck.repository.StockAdjustRepository;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.stockcheck.converter.StockAdjustPoConverter;
import com.somepro.infrastructure.persistence.stockcheck.po.StockAdjustPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 调账流水仓储适配器（基础设施层）。流水的写入在 StockCheckRepositoryImpl.adjustInTransaction（同事务），
 * 这里只提供按盘点单的查询。
 */
@Repository
public class StockAdjustRepositoryImpl implements StockAdjustRepository {

    private final StockAdjustMapper stockAdjustMapper;

    public StockAdjustRepositoryImpl(StockAdjustMapper stockAdjustMapper) {
        this.stockAdjustMapper = stockAdjustMapper;
    }



    @Override
    public Mono<StockAdjust> findByCheckId(Long checkId) {
        return blocking(() -> {
            StockAdjustPO po = stockAdjustMapper.selectOne(Wrappers.lambdaQuery(StockAdjustPO.class)
                    .eq(StockAdjustPO::getCheckId, checkId)
                    .last("LIMIT 1"));
            return po == null ? null : StockAdjustPoConverter.toDomain(po);
        });
    }

    /** 阻塞 DB 调用 → 响应式链路的桥接器（与 DemoItemRepositoryImpl 同一约定）。 */
    private <T> Mono<T> blocking(Supplier<T> supplier) {
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
