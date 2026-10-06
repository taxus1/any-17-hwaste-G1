package com.somepro.interfaces.rest.hwaste;

import com.somepro.application.hwaste.StockAdjustAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.hwaste.converter.StockAdjustVoConverter;
import com.somepro.interfaces.rest.hwaste.vo.PageVO;
import com.somepro.interfaces.rest.hwaste.vo.StockAdjustVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 库存调账流水接口（用户接口层）：只读查询，调账动作走盘点单的 /api/hwaste/check/adjust。
 */
@RestController
@RequestMapping("/api/hwaste/adjust")
public class StockAdjustController {

    private final StockAdjustAppService stockAdjustAppService;

    public StockAdjustController(StockAdjustAppService stockAdjustAppService) {
        this.stockAdjustAppService = stockAdjustAppService;
    }

    /** 调账流水分页查询：盘点单 / 单位 / 类别均可选。 */
    @GetMapping("/page")
    public Mono<Result<PageVO<StockAdjustVO>>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "20") int pageSize,
                                                    @RequestParam(required = false) Long checkId,
                                                    @RequestParam(required = false) Long sourceId,
                                                    @RequestParam(required = false) String categoryCode) {
        return stockAdjustAppService.page(pageNum, pageSize, checkId, sourceId, categoryCode)
                .map(StockAdjustVoConverter::toPageVo)
                .map(Result::ok);
    }
}
