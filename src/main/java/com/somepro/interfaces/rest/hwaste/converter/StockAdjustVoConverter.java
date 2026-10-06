package com.somepro.interfaces.rest.hwaste.converter;

import com.somepro.domain.hwaste.model.StockAdjust;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.hwaste.vo.PageVO;
import com.somepro.interfaces.rest.hwaste.vo.StockAdjustVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * StockAdjust（领域）→ 对外 VO 转换器（用户接口层）。
 */
public final class StockAdjustVoConverter {

    private StockAdjustVoConverter() {
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

    public static PageVO<StockAdjustVO> toPageVo(PageResult<StockAdjust> page) {
        List<StockAdjustVO> content = page.content().stream()
                .map(StockAdjustVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
