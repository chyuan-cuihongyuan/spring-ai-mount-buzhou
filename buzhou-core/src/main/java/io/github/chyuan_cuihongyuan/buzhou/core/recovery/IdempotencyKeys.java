package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

/**
 * 幂等键派生（spec「幂等三件套 ② 幂等键」/ CONTEXT「幂等键」）。纯函数。
 *
 * <p>去重记录以 {@code (sessionId, 幂等键)} 寻址 per-session 存储（{@code SessionStateStore}），
 * 故键本身只需保证<b>会话内唯一</b>（去重作用域 = 会话内，跨会话不归框架）：
 *
 * <ul>
 *   <li><b>框架默认键</b> = {@code dedup.<toolName>.<toolCallId>}——toolCallId 是模型每次调用
 *       下发的稳定 id，天然满足「会话 + 轮次 + 调用序号」唯一性。</li>
 *   <li><b>业务覆盖键</b> = {@code dedup.<toolName>.biz.<businessKey>}——业务工具经
 *       {@link IdempotencyKeyExtractor} 从入参取业务标识（如订单号）覆盖默认键，
 *       使语义幂等按正确业务粒度命中。</li>
 * </ul>
 */
public final class IdempotencyKeys {

    /** 去重记录键命名空间前缀（与 fact. / auth. 等会话 state 命名空间同级）。 */
    public static final String PREFIX = "dedup.";

    /** 业务覆盖键段标记。 */
    public static final String BUSINESS_SEGMENT = ".biz.";

    private IdempotencyKeys() {
    }

    /**
     * 框架默认幂等键。
     *
     * @param toolName   工具名
     * @param toolCallId 模型下发的工具调用 id（会话内稳定唯一）
     * @return 默认键
     */
    public static String defaultKey(String toolName, String toolCallId) {
        return PREFIX + toolName + "." + toolCallId;
    }

    /**
     * 业务覆盖幂等键。
     *
     * @param toolName    工具名
     * @param businessKey 业务标识（如订单号）
     * @return 业务覆盖键
     */
    public static String businessKey(String toolName, String businessKey) {
        return PREFIX + toolName + BUSINESS_SEGMENT + businessKey;
    }
}
