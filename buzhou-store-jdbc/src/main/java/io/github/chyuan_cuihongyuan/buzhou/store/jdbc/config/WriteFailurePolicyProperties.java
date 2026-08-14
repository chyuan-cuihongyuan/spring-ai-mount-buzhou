package io.github.chyuan_cuihongyuan.buzhou.store.jdbc.config;

import io.github.chyuan_cuihongyuan.buzhou.store.jdbc.WriteFailurePolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * store 横切写失败策略属性（spec 13 §stores-7 / ticket 32，前缀 {@code buzhou.store}）。
 *
 * <p>该属性按 spec 属于 store 横切面（jdbc / redis 实现共用同一键
 * {@code buzhou.store.write-failure-policy}），故单独绑定前缀根而非 {@code buzhou.store.jdbc}；
 * 两实现模块各自持有等价声明（模块间不互相依赖，与 JdbcJson/RedisJson 的既有惯例一致）。
 *
 * @param writeFailurePolicy {@code FAIL_TURN}（默认，既有外溢语义）/ {@code DEGRADE}
 *                           （观测类写降级 WARN + 计数继续，事实类写仍抛）
 */
@ConfigurationProperties(prefix = "buzhou.store")
public record WriteFailurePolicyProperties(WriteFailurePolicy writeFailurePolicy) {

    public WriteFailurePolicyProperties {
        writeFailurePolicy = writeFailurePolicy == null ? WriteFailurePolicy.FAIL_TURN : writeFailurePolicy;
    }
}
