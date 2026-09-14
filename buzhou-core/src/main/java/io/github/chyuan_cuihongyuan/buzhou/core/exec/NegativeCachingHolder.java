package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.time.Duration;
import org.springframework.ai.tool.ToolCallback;

/**
 * 工具失败负缓存 Holder（spec 1633 / T2417，spec 1616 装配面）：
 * enable 后 HarnessAssembler 对全部工具包 NegativeCachingToolCallback
 * （失败短 TTL 记忆防重试风暴）——进程级开关（默认关零包装零行为）。
 * @since 1.0.0
 */
public final class NegativeCachingHolder {

    private static volatile boolean enabled;
    private static volatile Duration ttl = Duration.ofSeconds(30);

    private NegativeCachingHolder() {
    }

    /** 启用/停用（停用后新会话不再包装——已包装会话的缓存自然过期）。 */
    public static void setEnabled(boolean value) {
        enabled = value;
    }

    /** 是否启用。 */
    public static boolean enabled() {
        return enabled;
    }

    /** 负 TTL（默认 30s——恢复窗口）。 */
    public static void setTtl(Duration value) {
        ttl = value == null || value.isNegative() || value.isZero()
                ? Duration.ofSeconds(30) : value;
    }

    /** 当前 TTL。 */
    public static Duration ttl() {
        return ttl;
    }

    /** 包装工具（未启用 = 原引用透传零开销）。 */
    public static ToolCallback wrap(ToolCallback delegate) {
        return enabled
                ? NegativeCachingToolCallback.wrap(delegate, ttl)
                : delegate;
    }
}
