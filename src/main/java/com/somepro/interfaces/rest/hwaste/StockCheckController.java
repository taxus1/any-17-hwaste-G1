package com.somepro.interfaces.rest.hwaste;

import com.somepro.application.hwaste.StockCheckAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.hwaste.converter.StockCheckVoConverter;
import com.somepro.interfaces.rest.hwaste.vo.PageVO;
import com.somepro.interfaces.rest.hwaste.vo.ReconcileVO;
import com.somepro.interfaces.rest.hwaste.vo.StockCheckVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 危废库存盘点单接口（用户接口层）：立单 → 开工 → 填实盘 → 审批 → 调账 / 作废，以及查询与对账。
 *
 * 状态流转类接口的盘点单定位参数：checkId 或 checkNo 传其一即可。
 */
@RestController
@RequestMapping("/api/hwaste/check")
public class StockCheckController {

    private final StockCheckAppService stockCheckAppService;

    public StockCheckController(StockCheckAppService stockCheckAppService) {
        this.stockCheckAppService = stockCheckAppService;
    }

    /** 立单：账面数按该单位该类别在库批次合计快照；同单位同类别同月份只准挂一份未作废的单。 */
    @PostMapping("/create")
    public Mono<Result<StockCheckVO>> create(@RequestParam(required = false) Long sourceId,
                                             @RequestParam(required = false) String categoryCode,
                                             @RequestParam(required = false) String checkPeriod) {
        return stockCheckAppService.create(sourceId, categoryCode, checkPeriod)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 开工：草稿 → 盘点中，记开工时刻；此后该单位该类别的新入库与联单转出被冻结。 */
    @PostMapping("/start")
    public Mono<Result<StockCheckVO>> start(@RequestParam(required = false) Long checkId,
                                            @RequestParam(required = false) String checkNo) {
        return stockCheckAppService.start(checkId, checkNo)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 填实盘：算差异定级别；正常档留盘点中可直接调账，重大/严重转待审批。 */
    @PostMapping("/count")
    public Mono<Result<StockCheckVO>> count(@RequestParam(required = false) Long checkId,
                                            @RequestParam(required = false) String checkNo,
                                            @RequestParam(required = false) BigDecimal countedWeight) {
        return stockCheckAppService.count(checkId, checkNo, countedWeight)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 批准：待审批 → 已批准。 */
    @PostMapping("/approve")
    public Mono<Result<StockCheckVO>> approve(@RequestParam(required = false) Long checkId,
                                              @RequestParam(required = false) String checkNo,
                                              @RequestParam(required = false) String approver) {
        return stockCheckAppService.approve(checkId, checkNo, approver)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 驳回：待审批 → 已驳回，必须写明理由。 */
    @PostMapping("/reject")
    public Mono<Result<StockCheckVO>> reject(@RequestParam(required = false) Long checkId,
                                             @RequestParam(required = false) String checkNo,
                                             @RequestParam(required = false) String reason) {
        return stockCheckAppService.reject(checkId, checkNo, reason)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 调账：把账拉平（在库合计 = 实盘），并留一条调账流水；同一单只调一次。 */
    @PostMapping("/adjust")
    public Mono<Result<StockCheckVO>> adjust(@RequestParam(required = false) Long checkId,
                                             @RequestParam(required = false) String checkNo,
                                             @RequestParam(required = false) String reason) {
        return stockCheckAppService.adjust(checkId, checkNo, reason)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 作废：终态；冻结随之解除。 */
    @PostMapping("/cancel")
    public Mono<Result<StockCheckVO>> cancel(@RequestParam(required = false) Long checkId,
                                             @RequestParam(required = false) String checkNo) {
        return stockCheckAppService.cancel(checkId, checkNo)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 单查明细：checkId 或 checkNo 传其一。 */
    @GetMapping("/detail")
    public Mono<Result<StockCheckVO>> detail(@RequestParam(required = false) Long checkId,
                                             @RequestParam(required = false) String checkNo) {
        return stockCheckAppService.detail(checkId, checkNo)
                .map(StockCheckVoConverter::toVo)
                .map(Result::ok);
    }

    /** 分页查询：单位 / 类别 / 月份 / 级别 / 状态均可选，都不传则分页列全。 */
    @GetMapping("/page")
    public Mono<Result<PageVO<StockCheckVO>>> page(@RequestParam(defaultValue = "1") int pageNum,
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

    /** 对账：给单位 + 类别 + 月份，回账面 / 实盘 / 差异 / 结存。 */
    @GetMapping("/reconcile")
    public Mono<Result<ReconcileVO>> reconcile(@RequestParam(required = false) Long sourceId,
                                               @RequestParam(required = false) String categoryCode,
                                               @RequestParam(required = false) String checkPeriod) {
        return stockCheckAppService.reconcile(sourceId, categoryCode, checkPeriod)
                .map(StockCheckVoConverter::toReconcileVo)
                .map(Result::ok);
    }
}
