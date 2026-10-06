package com.somepro.infrastructure.persistence.support;

import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.function.Supplier;

/**
 * 单据编号生成器（基础设施层）：形如 CK-2026-0001（前缀-当前年-四位起序号）。
 *
 * 取号口径：查该前缀当年已落库的最大号，序号 +1。synchronized 保证单实例内取号串行；
 * 号段唯一性最终由表上的唯一键（uk_check_no / uk_adjust_no / uk_batch_no / uk_manifest_no）兜底，
 * 调用方撞到 DuplicateKeyException 时重取重插即可（序号允许有空洞，不允许重复）。
 */
@Component
public class DocNoGenerator {

    /**
     * 取下一个号。
     *
     * @param prefix         编号前缀（CK / AJ / WB / EM / TP）
     * @param maxNoSupplier  查该前缀当年最大已落库编号的查询（在调用方锁内执行）
     */
    public synchronized String next(String prefix, Supplier<String> maxNoSupplier) {
        String year = String.valueOf(Year.now().getValue());
        String max = maxNoSupplier.get();
        int seq = 1;
        if (max != null && !max.isBlank()) {
            int dash = max.lastIndexOf('-');
            if (dash >= 0 && dash < max.length() - 1) {
                seq = Integer.parseInt(max.substring(dash + 1)) + 1;
            }
        }
        return prefix + "-" + year + "-" + String.format("%04d", seq);
    }

    /** 该前缀当年的 LIKE 前缀（如 CK-2026-），供调用方查最大号用。 */
    public String yearPrefix(String prefix) {
        return prefix + "-" + Year.now().getValue() + "-";
    }
}
