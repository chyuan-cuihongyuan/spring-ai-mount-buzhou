package io.github.chyuan_cuihongyuan.buzhou.core.leak;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 全局泄漏检测器持有者（impl-41 / spec 13 §T66）：默认 SIMPLE 级（1/128 采样）；
 * 装配层经 {@code buzhou.leak.*} 配置安装（DISABLED/ADVANCED/PARANOID + 出租时长阈值 +
 * LeakListener）。挂点（会话 / 租约 / spill 句柄）经本持有者取检测器——库内默认即零配置可用。
 */
public final class LeakDetectorHolder {

    private static final AtomicReference<ResourceLeakDetector> INSTANCE =
            new AtomicReference<>(new ResourceLeakDetector());

    private LeakDetectorHolder() {
    }

    public static ResourceLeakDetector detector() {
        return INSTANCE.get();
    }

    public static void install(ResourceLeakDetector detector) {
        INSTANCE.set(detector == null ? new ResourceLeakDetector() : detector);
    }

    /** 测试清理：恢复默认 SIMPLE 检测器。 */
    public static void reset() {
        INSTANCE.set(new ResourceLeakDetector());
    }
}
