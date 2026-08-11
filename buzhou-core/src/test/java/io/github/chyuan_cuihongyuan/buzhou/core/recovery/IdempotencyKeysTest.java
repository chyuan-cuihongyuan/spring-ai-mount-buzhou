package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 幂等键派生（纯函数决策表，spec「幂等三件套 ②」）。 */
class IdempotencyKeysTest {

    @Test
    void defaultKeyIsSessionScopedViaToolCallId() {
        // 框架默认键 = 工具名 + toolCallId（toolCallId 天然含「会话 + 轮次 + 调用序号」唯一性）
        assertThat(IdempotencyKeys.defaultKey("charge", "tc-1"))
                .isEqualTo("dedup.charge.tc-1");
        // 同会话内不同调用序号 → 不同键
        assertThat(IdempotencyKeys.defaultKey("charge", "tc-2"))
                .isNotEqualTo(IdempotencyKeys.defaultKey("charge", "tc-1"));
        // 不同工具同调用 id → 不同键（工具名在命名空间内）
        assertThat(IdempotencyKeys.defaultKey("refund", "tc-1"))
                .isNotEqualTo(IdempotencyKeys.defaultKey("charge", "tc-1"));
    }

    @Test
    void businessKeyOverridesDefaultWithBusinessIdentity() {
        // 业务覆盖键 = 工具名 + 业务标识（如订单号），同订单号跨调用命中同键
        assertThat(IdempotencyKeys.businessKey("charge", "ORD-1"))
                .isEqualTo("dedup.charge.biz.ORD-1");
        assertThat(IdempotencyKeys.businessKey("charge", "ORD-1"))
                .isNotEqualTo(IdempotencyKeys.defaultKey("charge", "tc-1"));
    }
}
