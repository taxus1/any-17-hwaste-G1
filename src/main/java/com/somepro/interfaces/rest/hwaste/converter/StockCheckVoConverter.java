package com.somepro.interfaces.rest.hwaste.converter;

import com.somepro.domain.hwaste.model.StockCheck;
import com.somepro.domain.hwaste.model.StockReconciliation;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.hwaste.vo.PageVO;
import com.somepro.interfaces.rest.hwaste.vo.ReconcileVO;
import com.somepro.interfaces.rest.hwaste.vo.StockCheckVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * StockCheck（领域）→ 对外 VO 转换器（用户接口层）。
 */
public final class StockCheckVoConverter {

    private StockCheckVoConverter() {
    }

    public static StockCheckVO toVo(StockCheck domain) {
        return new StockCheckVO(
                domain.getId(),
                domain.getCheckNo(),
                domain.getSourceId(),
                domain.getCategoryCode(),
                domain.getCheckPeriod(),
                domain.getBookWeight(),
                domain.getCountedWeight(),
                domain.getDiffWeight(),
                domain.getDiffRatio(),
                domain.getCheckLevel() == null ? null : domain.getCheckLevel().name(),
                domain.getStatus() == null ? null : domain.getStatus().name(),
                domain.getStartedAt(),
                domain.getFinishedAt(),
                domain.getApprover(),
                domain.getApprovedAt(),
                domain.getRejectReason(),
                domain.getCreateTime());
    }

    public static PageVO<StockCheckVO> toPageVo(PageResult<StockCheck> page) {
        List<StockCheckVO> content = page.content().stream()
                .map(StockCheckVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }

    public static ReconcileVO toReconcileVo(StockReconciliation domain) {
        return new ReconcileVO(
                domain.checkNo(),
                domain.sourceId(),
                domain.categoryCode(),
                domain.checkPeriod(),
                domain.status() == null ? null : domain.status().name(),
                domain.bookWeight(),
                domain.countedWeight(),
                domain.diffWeight(),
                domain.balanceWeight());
    }
}
