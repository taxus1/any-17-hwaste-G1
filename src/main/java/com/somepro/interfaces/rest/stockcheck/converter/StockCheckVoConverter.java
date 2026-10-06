package com.somepro.interfaces.rest.stockcheck.converter;

import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.stockcheck.model.ReconcileResult;
import com.somepro.domain.stockcheck.model.StockAdjust;
import com.somepro.domain.stockcheck.model.StockCheck;
import com.somepro.interfaces.rest.stockcheck.vo.PageVO;
import com.somepro.interfaces.rest.stockcheck.vo.ReconcileVO;
import com.somepro.interfaces.rest.stockcheck.vo.StockAdjustVO;
import com.somepro.interfaces.rest.stockcheck.vo.StockCheckVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 盘点领域对象 → 对外 VO 转换器（用户接口层）。
 * Controller 不许直接返回领域对象，一律经这里转成 VO。
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

    public static StockAdjustVO toVo(StockAdjust domain) {
        return new StockAdjustVO(
                domain.getId(),
                domain.getAdjustNo(),
                domain.getCheckId(),
                domain.getSourceId(),
                domain.getCategoryCode(),
                domain.getAdjustWeight(),
                domain.getReason(),
                domain.getAdjustedAt(),
                domain.getCreateTime());
    }

    public static ReconcileVO toVo(ReconcileResult domain) {
        return new ReconcileVO(
                domain.checkNo(),
                domain.sourceId(),
                domain.categoryCode(),
                domain.checkPeriod(),
                domain.bookWeight(),
                domain.countedWeight(),
                domain.diffWeight(),
                domain.adjustWeight(),
                domain.balanceWeight(),
                domain.status() == null ? null : domain.status().name());
    }

    public static PageVO<StockCheckVO> toPageVo(PageResult<StockCheck> page) {
        List<StockCheckVO> content = page.content().stream()
                .map(StockCheckVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
