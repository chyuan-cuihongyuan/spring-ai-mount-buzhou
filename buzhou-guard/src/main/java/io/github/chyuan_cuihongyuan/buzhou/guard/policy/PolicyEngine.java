package io.github.chyuan_cuihongyuan.buzhou.guard.policy;

import java.util.List;

/**
 * 授权策略引擎 SPI（wayfinder2 impl-23 / T52 / docs/spec/12 §guard-24）：
 * 默认拒、决策附 reason——危险工具门配置的泛化。
 *
 * <p>实现：内嵌 {@link EmbeddedPolicyEngine}（自有可分析子集，默认开箱即用）；
 * <b>OPA sidecar adapter</b>（opa-java optional 依赖、探测式启用）为要 Rego 全表达力的
 * 部署方预留（本切片交付接口与内嵌实现，adapter 依赖按部署需求另接——研究：OPA 12.1K★
 * 达标但 JVM 无成熟内嵌，官方 opa-java 仅 REST 客户端）。cedar-java（75★）注记备选不引入。
 */
public interface PolicyEngine {

    /**
     * 裁决：首条命中规则的决策生效；<b>无命中 = 默认拒</b>；ESCALATE 且已人工审批 = allow
     * （approver 通道语义，与既有授权台账一致）。
     */
    PolicyDecision decide(PolicyDecision.Input input);
}
