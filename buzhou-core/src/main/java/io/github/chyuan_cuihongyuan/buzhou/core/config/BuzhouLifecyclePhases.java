package io.github.chyuan_cuihongyuan.buzhou.core.config;

/**
 * Buzhou 各机制 {@link org.springframework.context.SmartLifecycle} 的 phase 常量集中声明
 * （impl-30 / spec 13 §core-1 优雅停机与生命周期）。
 *
 * <h2>语义：Spring stop 反序</h2>
 * <p>Spring {@code DefaultLifecycleProcessor} 按 phase <b>升序 start、降序 stop</b>——
 * phase 数值<b>越大越先 stop</b>。因此分配原则：
 *
 * <ul>
 *   <li><b>core 会话/执行层 phase 最大（{@link #CORE}，最先 stop）</b>：停机序列 = 拒绝新
 *       Turn → 对全部在途会话发 {@code AFTER_CURRENT_TURN} 取消 → 排空等待 → 超时硬截断
 *       （预算 {@code buzhou.lifecycle.timeout-per-shutdown-phase}，默认 30s）。</li>
 *   <li><b>memory / spill / guard 后台与缓存层 phase 较小（后 stop）</b>：core 停机会话后，
 *       机制层才关闭各自的后台任务与缓存（如 memory 的 sleep-time 调度器）。</li>
 *   <li><b>持久层语义最后撤离（{@link #STORE} 最小）</b>：为 store 模块预留更小 phase 空间——
 *       前面各层停机时的落库/flush 仍可用；本片（切片 30）不实装 store 侧 lifecycle，
 *       数值预留即契约。</li>
 * </ul>
 *
 * <p>数值间隔 1000，层内可再细分（如 store 0~999、guard 1000~1999）。core 用
 * {@code Integer.MAX_VALUE} 与 Spring 默认 phase 持平（同为默认值的第三方 lifecycle 与 core
 * 之间的相对顺序不保证——core 停机序列自身闭环，不依赖与外部的相对顺序）。
 */
public final class BuzhouLifecyclePhases {

    private BuzhouLifecyclePhases() {
    }

    /**
     * core 会话/执行层（最先 stop）：拒绝新 Turn → 在途 AFTER_CURRENT_TURN 取消 →
     * 排空等待 → 超时硬截断 → 关闭全部会话（释放租约、清空资源注册表）。
     */
    public static final int CORE = Integer.MAX_VALUE;

    /**
     * memory 后台与缓存层：关闭 sleep-time 整理调度器等模块自有后台任务
     * （深度治理——pending 队列上限、会话结束摘除——属切片 38）。
     */
    public static final int MEMORY = 3_000;

    /**
     * spill 句柄注册表/缓存层。本片诚实边界：spill 侧无可关闭资源
     * （{@code HandleLifecycleRegistry} / {@code SessionReadOnlyRegistry} /
     * {@code SemanticChunkIndex} 均为进程内 ConcurrentHashMap，无 flush 语义），
     * lifecycle 仅做 phase 声明与占位。
     */
    public static final int SPILL = 2_000;

    /**
     * guard 审计链层。本片诚实边界：审计链（{@code AuditChain}）由应用经
     * {@code SpawnOptions.withListeners} 自持、未在装配面接线，持久化属切片 39——
     * lifecycle 仅做 phase 声明与占位（审计 flush 钩子在切片 39 接线时落位于此）。
     */
    public static final int GUARD = 1_000;

    /**
     * 持久层（最后撤离）：为 store 模块（buzhou-store-jdbc / redis）预留的 phase 空间下限。
     * 本片不实装 store 侧 lifecycle；数值即契约——各层停机时 store 仍可写入。
     */
    public static final int STORE = 0;
}
