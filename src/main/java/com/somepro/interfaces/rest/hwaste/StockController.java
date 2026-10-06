package com.somepro.interfaces.rest.hwaste;

import com.somepro.application.hwaste.StockAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.hwaste.converter.WasteStockVoConverter;
import com.somepro.interfaces.rest.hwaste.vo.PageVO;
import com.somepro.interfaces.rest.hwaste.vo.TransferResultVO;
import com.somepro.interfaces.rest.hwaste.vo.WasteStockVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 危废入库批次接口（用户接口层）：新入库、联单转出、在库合计与批次分页。
 *
 * 盘点冻结规则：该单位该类别有单子在盘点中（COUNTING / PENDING_APPROVAL / APPROVED）时，
 * 新入库与联单转出都先挡回；没在盘点中的组合照常放行。
 */
@RestController
@RequestMapping("/api/hwaste/stock")
public class StockController {

    private final StockAppService stockAppService;

    public StockController(StockAppService stockAppService) {
        this.stockAppService = stockAppService;
    }

    /** 新入库：生成 WB 批次，落在库状态。 */
    @PostMapping("/inbound")
    public Mono<Result<WasteStockVO>> inbound(@RequestParam(required = false) Long sourceId,
                                              @RequestParam(required = false) String categoryCode,
                                              @RequestParam(required = false) BigDecimal weightKg,
                                              @RequestParam(required = false) String packageType) {
        return stockAppService.inbound(sourceId, categoryCode, weightKg, packageType)
                .map(WasteStockVoConverter::toVo)
                .map(Result::ok);
    }

    /** 联单转出：按入库先后 FIFO 消化在库批次，不足整批的拆分子批。 */
    @PostMapping("/transfer-out")
    public Mono<Result<TransferResultVO>> transferOut(@RequestParam(required = false) Long sourceId,
                                                      @RequestParam(required = false) String categoryCode,
                                                      @RequestParam(required = false) BigDecimal weightKg) {
        return stockAppService.transferOut(sourceId, categoryCode, weightKg)
                .map(transferred -> new TransferResultVO(sourceId, categoryCode, transferred))
                .map(Result::ok);
    }

    /** 该单位该类别当前在库重量合计。 */
    @GetMapping("/sum")
    public Mono<Result<BigDecimal>> sumInStock(@RequestParam(required = false) Long sourceId,
                                               @RequestParam(required = false) String categoryCode) {
        return stockAppService.sumInStock(sourceId, categoryCode).map(Result::ok);
    }

    /** 批次分页查询：单位 / 类别 / 状态均可选。 */
    @GetMapping("/page")
    public Mono<Result<PageVO<WasteStockVO>>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "20") int pageSize,
                                                   @RequestParam(required = false) Long sourceId,
                                                   @RequestParam(required = false) String categoryCode,
                                                   @RequestParam(required = false) String status) {
        return stockAppService.page(pageNum, pageSize, sourceId, categoryCode, status)
                .map(WasteStockVoConverter::toPageVo)
                .map(Result::ok);
    }
}
