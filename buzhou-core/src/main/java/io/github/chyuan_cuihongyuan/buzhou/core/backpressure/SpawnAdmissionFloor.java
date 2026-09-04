package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * spawn 准入地板槽（spec 335 / T661；spec 342 / T675 升级多源合成）：
 * named source 各自 set——{@link #get()} 返回各源<b>语义最高</b>（序数
 * 最小）者，无源默认 {@link SpawnPriority#LOW}（全放行，零变化）。
 *
 * <p><b>多源正交</b>（spec 342）：维护 cordon（maintenance 源）与预算
 * 冻结（default 源——335 ErrorBudgetPolicy 调 {@link #set(SpawnPriority)}
 * 零改动）同向抬 HIGH 时互不干扰：谁先结束谁落回自己的源，另一方仍
 * 生效——单写者互踩（cordon 结束压掉仍在烧穿的冻结）就此消除。
 */
public final class SpawnAdmissionFloor implements Supplier<SpawnPriority> {

    /** 335 兼容源（单参 set 的归属）。 */
    public static final String DEFAULT_SOURCE = "default";

    private final ConcurrentHashMap<String, SpawnPriority> sources = new ConcurrentHashMap<>();

    /** 335 兼容：default 源写（null 折 LOW）。 */
    public void set(SpawnPriority priority) {
        set(DEFAULT_SOURCE, priority);
    }

    /** 源级写（null = 该源落回移除；防御面折 LOW 语义等价）。 */
    public void set(String source, SpawnPriority priority) {
        if (source == null || source.isBlank()) {
            return;
        }
        if (priority == null) {
            sources.remove(source);
            return;
        }
        sources.put(source, priority);
    }

    /** 各源语义最高（序数最小）；无源 LOW。 */
    @Override
    public SpawnPriority get() {
        SpawnPriority highest = SpawnPriority.LOW;
        for (SpawnPriority priority : sources.values()) {
            if (priority != null && priority.ordinal() < highest.ordinal()) {
                highest = priority;
            }
        }
        return highest;
    }

    /** 各源视图（观测面）。 */
    public Map<String, SpawnPriority> view() {
        return Map.copyOf(sources);
    }
}
