package com.somepro.interfaces.rest.stockcheck;

import com.somepro.application.stockcheck.StockCheckAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.stockcheck.converter.StockCheckVoConverter;
import com.somepro.interfaces.rest.stockcheck.vo.PageVO;
import com.somepro.interfaces.rest.stockcheck.vo.ReconcileVO;
import com.somepro.interfaces.rest.stockcheck.vo.StockAdjustVO;
import com.somepro.interfaces.rest.stockcheck.vo.StockCheckVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 库存盘点与盈亏调账接口（用户接口层）：只做协议适配与 VO 转换，业务编排在应用层。
 *
 * 流程：create 立单（DRAFT）→ start 开工（COUNTING，冻结进出）→ count 填实盘
 * （正常档直接可调账，重大/严重进 PENDING_APPROVAL）→ approve/reject → adjust 调账（ADJUSTED）
 * 或 cancel 作废（CANCELLED）；reconcile 对账。
 */
@RestController
@RequestMapping("/api/stockcheck")
public class StockCheckController {

    private final StockCheckAppService stockCheckAppService;

    public StockCheckController(StockCheckAppService stockCheckAppService) {
        this.stockCheckAppService = stockCheckAppService;
    }

    /** 立单：账面重量按该单位+类别当前在库批次合计取数；同组合未作废只准一张。 */
    @PostMapping("/create")
    public Mono<Result<StockCheckVO>> create(@RequestParam Long sourceId,
                                             @RequestParam String categoryCode,
                                             @RequestParam String checkPeriod) {
        return stockCheckAppService.create(sourceId, categoryCode, checkPeriod)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 开工：DRAFT → COUNTING，记开工时刻；此后该单位+类别冻结入库与转出。 */
    @PostMapping("/start")
    public Mono<Result<StockCheckVO>> start(@RequestParam Long id) {
        return stockCheckAppService.start(id)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 填实盘：算差异/比例/级别；正常档免审直接可调账，重大/严重送待审批。 */
    @PostMapping("/count")
    public Mono<Result<StockCheckVO>> count(@RequestParam Long id,
                                            @RequestParam BigDecimal countedWeight) {
        return stockCheckAppService.count(id, countedWeight)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 批准：PENDING_APPROVAL → APPROVED。 */
    @PostMapping("/approve")
    public Mono<Result<StockCheckVO>> approve(@RequestParam Long id,
                                              @RequestParam(required = false) String approver) {
        return stockCheckAppService.approve(id, approver)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 驳回：PENDING_APPROVAL → REJECTED，理由必填。 */
    @PostMapping("/reject")
    public Mono<Result<StockCheckVO>> reject(@RequestParam Long id,
                                             @RequestParam String reason,
                                             @RequestParam(required = false) String approver) {
        return stockCheckAppService.reject(id, approver, reason)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 调账：把账拉平（在库合计=实盘），留一条调账流水；同一张单只调一次。 */
    @PostMapping("/adjust")
    public Mono<Result<StockAdjustVO>> adjust(@RequestParam Long id,
                                              @RequestParam(required = false) String reason) {
        return stockCheckAppService.adjust(id, reason)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 作废：非终态 → CANCELLED（终态），释放该单位+类别的进出冻结。 */
    @PostMapping("/cancel")
    public Mono<Result<StockCheckVO>> cancel(@RequestParam Long id) {
        return stockCheckAppService.cancel(id)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 详情。 */
    @GetMapping("/detail")
    public Mono<Result<StockCheckVO>> detail(@RequestParam Long id) {
        return stockCheckAppService.detail(id)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 分页查询：单位/类别/月份/级别/状态都可挑，啥都不挑就分页列全。 */
    @GetMapping("/list")
    public Mono<Result<PageVO<StockCheckVO>>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "20") int pageSize,
                                                   @RequestParam(required = false) Long sourceId,
                                                   @RequestParam(required = false) String categoryCode,
                                                   @RequestParam(required = false) String checkPeriod,
                                                   @RequestParam(required = false) String checkLevel,
                                                   @RequestParam(required = false) String status) {
        return stockCheckAppService.page(pageNum, pageSize, sourceId, categoryCode, checkPeriod, checkLevel, status)
                .map(StockCheckVoConverter::toPageVo)
                .map(Result::ok);
    }

    /** 对账：给单位+类别+月份，回账面、实盘、差异、调账额与调完账后的结存。 */
    @GetMapping("/reconcile")
    public Mono<Result<ReconcileVO>> reconcile(@RequestParam Long sourceId,
                                               @RequestParam String categoryCode,
                                               @RequestParam String checkPeriod) {
        return stockCheckAppService.reconcile(sourceId, categoryCode, checkPeriod)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }
}
