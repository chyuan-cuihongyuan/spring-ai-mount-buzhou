package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * spawn 准入地板槽（spec 335 / T661，Google SRE error budget policy 借鉴）：
 * 共享 volatile 优先级——默认 {@link SpawnPriority#LOW}（全放行，零变化），
 * 政策驱动 {@code set}、SpawnGate 读 {@code get}。
 *
 * <p><b>为什么共享槽而非闭包直连</b>：gate 在 runtime 装配期构造（局部），
 * 政策是独立 SmartLifecycle bean——槽解耦两者装配顺序，谁先谁后都成立。
 */
public final class SpawnAdmissionFloor implements Supplier<SpawnPriority> {

    private final AtomicReference<SpawnPriority> floor =
            new AtomicReference<>(SpawnPriority.LOW);

    @Override
    public SpawnPriority get() {
        return floor.get();
    }

    /** 抬/降地板（null 折 LOW——防御面）。 */
    public void set(SpawnPriority priority) {
        floor.set(priority == null ? SpawnPriority.LOW : priority);
    }
}
