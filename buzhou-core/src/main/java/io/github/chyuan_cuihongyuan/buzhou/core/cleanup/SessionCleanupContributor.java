package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import java.util.function.Consumer;

/**
 * impl-35 / spec 13 §stores-6：级联清理的挂接点——store 之外的会话级数据
 * （spill 文件、embedding 缓存等）以此形状并入 {@link SessionCleaner} 的一次级联。
 *
 * <p>经 {@code RuntimeConfig.sessionCleanupContributors()} 汇入运行时装配
 * （{@code RecoverySupport.attach} 即用本形状挂 run_registry / tool_call_log）。
 *
 * @param name     目标名（进 {@link SessionCleanupResult} 报告；重复名后者覆盖前者）
 * @param deletion 按 sessionId 执行删除（实现须幂等）
 */
public record SessionCleanupContributor(String name, Consumer<String> deletion) {

    public SessionCleanupContributor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("SessionCleanupContributor.name 不能为空");
        }
        if (deletion == null) {
            throw new IllegalArgumentException("SessionCleanupContributor.deletion 不能为空");
        }
    }

    /** 便捷工厂：方法引用直接挂接。 */
    public static SessionCleanupContributor of(String name, Consumer<String> deletion) {
        return new SessionCleanupContributor(name, deletion);
    }
}
