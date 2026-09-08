package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

/**
 * 在线采样入评测集 hook（spec 407 / T705，Honeycomb head-based deterministic
 * sampling 借鉴）：afterTurn 尾观察（order 900 不拦不改）——确定性
 * {@code floorMod(hash(sessionId:turn),100) < rate-percent}（同轮同判可复现）；
 * 空白/短问过滤；采样即 addItem（provenance 与拉式回流同溯源域）；异常
 * fail-soft（采样是旁路绝不炸轮）+ 双计数。采样语义 = 进候选池——golden
 * 与否仍是人工判断（与 SessionTrajectoryImporter 同口径）。
 */
public final class TurnSamplerHook implements BuzhouHook {

    /** 尾观察位（晚于既有观察 hook——纯旁路）。 */
    public static final int ORDER = 900;

    /** 采样策略（dataset 必须预建——采样不建集）。 */
    public record Policy(String dataset, int ratePercent, int minInputChars) {

        public Policy {
            if (dataset == null || dataset.isBlank()) {
                throw new IllegalArgumentException("dataset 非空");
            }
            if (ratePercent < 0 || ratePercent > 100) {
                throw new IllegalArgumentException("rate-percent 取值 [0,100]：" + ratePercent);
            }
            minInputChars = Math.max(0, minInputChars);
        }
    }

    private final EvalDatasetStore datasetStore;
    private final Policy policy;

    public TurnSamplerHook(EvalDatasetStore datasetStore, Policy policy) {
        this.datasetStore = datasetStore;
        this.policy = policy;
    }

    @Override
    public String name() {
        return "TurnSamplerHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult afterTurn(TurnContext ctx) {
        if (policy.ratePercent() <= 0) {
            return HookResult.CONTINUE;
        }
        String input = ctx.input();
        String response = ctx.response();
        if (input == null || input.isBlank() || response == null || response.isBlank()) {
            return HookResult.CONTINUE; // 缺问或缺答——无可采内容
        }
        if (input.length() < policy.minInputChars()) {
            return HookResult.CONTINUE; // 短问过滤
        }
        if (!sampled(ctx.sessionId(), ctx.turn())) {
            return HookResult.CONTINUE;
        }
        try {
            datasetStore.addItem(policy.dataset(), input, response, ctx.sessionId(), ctx.turn());
            BuzhouMetricsHolder.metrics().counter("buzhou.eval.sampled-turns");
        } catch (RuntimeException e) {
            // fail-soft：数据集未建等宿主漏配——计数暴露，绝不炸轮
            BuzhouMetricsHolder.metrics().counter("buzhou.eval.sampling-failed");
        }
        return HookResult.CONTINUE;
    }

    /** 确定性采样判定（同轮同判——重放不改变决定）。 */
    boolean sampled(String sessionId, int turn) {
        if (policy.ratePercent() >= 100) {
            return true;
        }
        int bucket = Math.floorMod((sessionId + ":" + turn).hashCode(), 100);
        return bucket < policy.ratePercent();
    }

    Policy policy() {
        return policy;
    }
}
