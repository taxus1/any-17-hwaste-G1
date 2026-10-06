package com.somepro.infrastructure.persistence.hwaste;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.hwaste.model.CheckStatus;
import com.somepro.domain.hwaste.model.StockCheck;
import com.somepro.domain.hwaste.repository.StockCheckRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.persistence.base.BaseBlockingRepository;
import com.somepro.infrastructure.persistence.hwaste.converter.StockCheckPoConverter;
import com.somepro.infrastructure.persistence.hwaste.po.StockCheckPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 盘点单仓储适配器（基础设施层）。
 *
 * 并发约定：立单的「查重 + 取号 + 插入」包在同一把 CK 锁里 ——
 * 两个人几乎同时给同一家单位同一个类别立单，只该成一份。
 */
@Repository
public class StockCheckRepositoryImpl extends BaseBlockingRepository implements StockCheckRepository {

    private final StockCheckMapper stockCheckMapper;
    private final BizNoService bizNoService;

    public StockCheckRepositoryImpl(StockCheckMapper stockCheckMapper, BizNoService bizNoService) {
        this.stockCheckMapper = stockCheckMapper;
        this.bizNoService = bizNoService;
    }

    @Override
    public Mono<StockCheck> create(StockCheck check) {
        return blocking(() -> bizNoService.inLock("CK", () -> {
            Long dup = stockCheckMapper.selectCount(Wrappers.<StockCheckPO>lambdaQuery()
                    .eq(StockCheckPO::getSourceId, check.getSourceId())
                    .eq(StockCheckPO::getCategoryCode, check.getCategoryCode())
                    .eq(StockCheckPO::getCheckPeriod, check.getCheckPeriod())
                    .ne(StockCheckPO::getStatus, CheckStatus.CANCELLED.name()));
            if (dup != null && dup > 0) {
                throw new BizException("该单位该类别该月份已存在未作废的盘点单，请勿重复立单");
            }
            StockCheckPO po = StockCheckPoConverter.toPo(check);
            po.setId(IdUtil.getSnowflakeNextId());
            po.setCheckNo(bizNoService.nextCheckNo());
            stockCheckMapper.insert(po);
            return StockCheckPoConverter.toDomain(po);
        }));
    }

    @Override
    public Mono<StockCheck> findById(Long id) {
        return blocking(() -> {
            StockCheckPO po = stockCheckMapper.selectById(id);
            return po == null ? null : StockCheckPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<StockCheck> findByCheckNo(String checkNo) {
        return blocking(() -> {
            StockCheckPO po = stockCheckMapper.selectOne(Wrappers.<StockCheckPO>lambdaQuery()
                    .eq(StockCheckPO::getCheckNo, checkNo));
            return po == null ? null : StockCheckPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<StockCheck> save(StockCheck check) {
        return blocking(() -> {
            StockCheckPO po = StockCheckPoConverter.toPo(check);
            stockCheckMapper.updateById(po);
            return StockCheckPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PageResult<StockCheck>> page(int pageNum, int pageSize, Long sourceId, String categoryCode,
                                             String checkPeriod, String checkLevel, String status) {
        return this.<PageResult<StockCheck>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<StockCheckPO> wrapper = Wrappers.<StockCheckPO>lambdaQuery()
                        .eq(sourceId != null, StockCheckPO::getSourceId, sourceId)
                        .eq(categoryCode != null && !categoryCode.isBlank(),
                                StockCheckPO::getCategoryCode, categoryCode)
                        .eq(checkPeriod != null && !checkPeriod.isBlank(),
                                StockCheckPO::getCheckPeriod, checkPeriod)
                        .eq(checkLevel != null && !checkLevel.isBlank(),
                                StockCheckPO::getCheckLevel, checkLevel)
                        .eq(status != null && !status.isBlank(), StockCheckPO::getStatus, status)
                        .orderByDesc(StockCheckPO::getId);
                List<StockCheckPO> rows = stockCheckMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<StockCheck> content = rows.stream()
                        .map(StockCheckPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<StockCheck> findLatestActive(Long sourceId, String categoryCode, String checkPeriod) {
        return blocking(() -> {
            StockCheckPO po = stockCheckMapper.selectOne(Wrappers.<StockCheckPO>lambdaQuery()
                    .eq(StockCheckPO::getSourceId, sourceId)
                    .eq(StockCheckPO::getCategoryCode, categoryCode)
                    .eq(StockCheckPO::getCheckPeriod, checkPeriod)
                    .ne(StockCheckPO::getStatus, CheckStatus.CANCELLED.name())
                    .orderByDesc(StockCheckPO::getId)
                    .last("LIMIT 1"));
            return po == null ? null : StockCheckPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<Long> countFreezing(Long sourceId, String categoryCode) {
        return blocking(() -> stockCheckMapper.selectCount(Wrappers.<StockCheckPO>lambdaQuery()
                .eq(StockCheckPO::getSourceId, sourceId)
                .eq(StockCheckPO::getCategoryCode, categoryCode)
                .in(StockCheckPO::getStatus, CheckStatus.COUNTING.name(),
                        CheckStatus.PENDING_APPROVAL.name(), CheckStatus.APPROVED.name())));
    }
}
