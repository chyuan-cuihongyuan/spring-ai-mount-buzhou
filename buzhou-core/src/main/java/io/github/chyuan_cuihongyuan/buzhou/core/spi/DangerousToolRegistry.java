package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程级危险工具名注册表（spec 1508 / T2267，design-incompleteness S2 修复）——
 * 模块解耦下的装配期桥：危险工具的供给方（tools autoconfig 等）装配后灌注，
 * 消费方（guard autoconfig）构建 HITL 守卫时并入默认条目（yml 显式条目优先）。
 * EvalRunRegistry 同款静态注册表模式；{@link #reset()} 测试隔离用。
 */
public final class DangerousToolRegistry {

    private static final Set<String> REGISTERED = ConcurrentHashMap.newKeySet();

    private DangerousToolRegistry() {
    }

    /** 灌注一批危险工具名（并集；幂等）。 */
    public static void register(Set<String> toolNames) {
        if (toolNames != null) {
            REGISTERED.addAll(toolNames);
        }
    }

    /** 当前注册快照（不可变）。 */
    public static Set<String> registered() {
        return Set.copyOf(REGISTERED);
    }

    /** 清零（测试隔离用）。 */
    public static void reset() {
        REGISTERED.clear();
    }
}
