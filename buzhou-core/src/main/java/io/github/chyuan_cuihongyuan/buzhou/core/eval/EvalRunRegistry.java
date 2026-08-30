package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 活跃评估 run 注册表（spec 77 §A / T307，LangSmith active-runs 观测面借鉴；
 * BuzhouMetricsHolder 同款全局旋钮模式——Netty ResourceLeakDetector 先例）：
 * {@link EvalRunner}（kind={@value #KIND_EVAL}）与 {@link PairwiseEvalRunner}
 * （kind={@value #KIND_AB}）在 run 生命周期内 begin/close 维护在飞计数；每 kind
 * 首次 begin 注册 gauge {@code buzhou.eval.runs.active}（tag kind）——不装
 * micrometer 时 no-op 零开销。runId 幂等（重复 begin 同 id 只计一次）。
 */
public final class EvalRunRegistry {

    public static final String KIND_EVAL = "eval";
    public static final String KIND_AB = "ab";

    static final String GAUGE_NAME = "buzhou.eval.runs.active";

    private static final AtomicReference<EvalRunRegistry> GLOBAL =
            new AtomicReference<>(new EvalRunRegistry());

    private final Map<String, Set<String>> activeByKind = new ConcurrentHashMap<>();
    private final Set<String> gaugeRegisteredKinds = ConcurrentHashMap.newKeySet();

    private EvalRunRegistry() {
    }

    /** 独立实例（测试/宿主自管作用域用；默认走 {@link #global()}）。 */
    public static EvalRunRegistry create() {
        return new EvalRunRegistry();
    }

    public static EvalRunRegistry global() {
        return GLOBAL.get();
    }

    /** 测试替换全局实例（null = 换新；@AfterEach 清理纪律）。 */
    public static void install(EvalRunRegistry registry) {
        GLOBAL.set(registry == null ? new EvalRunRegistry() : registry);
    }

    /** 登记在飞 run（幂等：同 runId 重复 begin 只计一次）。 */
    public Registration begin(String kind, String runId) {
        Set<String> ids = activeByKind.computeIfAbsent(kind, k -> ConcurrentHashMap.newKeySet());
        ids.add(runId);
        if (gaugeRegisteredKinds.add(kind)) {
            BuzhouMetricsHolder.metrics().gauge(GAUGE_NAME, ids::size, "kind", kind);
        }
        return new Registration(ids, runId);
    }

    /** 该 kind 当前在飞 run 数。 */
    public int active(String kind) {
        Set<String> ids = activeByKind.get(kind);
        return ids == null ? 0 : ids.size();
    }

    /** 全部 kind 在飞快照（只读视图——观测面调试用）。 */
    public Map<String, Integer> activeSnapshot() {
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        activeByKind.forEach((kind, ids) -> out.put(kind, ids.size()));
        return java.util.Collections.unmodifiableMap(out);
    }

    /** 注销句柄（close 幂等、线程安全；try-with-resources 友好）。 */
    public static final class Registration implements AutoCloseable {

        private final Set<String> ids;
        private final String runId;
        private boolean closed;

        private Registration(Set<String> ids, String runId) {
            this.ids = ids;
            this.runId = runId;
        }

        @Override
        public synchronized void close() {
            if (!closed) {
                closed = true;
                ids.remove(runId);
            }
        }
    }
}
