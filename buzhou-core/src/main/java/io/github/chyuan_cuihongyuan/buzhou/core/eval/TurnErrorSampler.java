package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;

/**
 * 错误偏向采样观察者（spec 423 / T737，OTel tail_sampling「ERROR 全保」
 * 规则借鉴）：错误轮<b>不走 afterTurn</b>（failTurnOnce 只回调
 * onTurnError）——407 均匀采样面看不见失败轮。本观察者在 onTurnStart
 * 记输入、onTurnError 按错误率确定性采样入集（onTurnEnd 清场——成功路
 * 归 {@link TurnSamplerHook}）。
 *
 * <p>入集 response 占位 {@code [TURN-ERROR] 异常类: 消息}（消息 ≤200 截断）
 * ——采样语义 = 进候选池，golden 与否仍是人工判断（407 同口径）。
 * per-session 实例（装配 customizer 每会话 new）；当前输入普通字段——
 * reactor 信号串行化保证 onTurnStart/onTurnError 相互 happens-before。
 * fail-soft：入集异常吞 + 计数（采样是旁路绝不炸轮）。
 */
public final class TurnErrorSampler implements SessionObserver {

    /** 错误摘要占位前缀（评测集内肉眼可辨「这不是模型产出」）。 */
    public static final String PLACEHOLDER_PREFIX = "[TURN-ERROR] ";

    private static final int MAX_MESSAGE_CHARS = 200;

    /** 错误采样策略（dataset 必须预建——采样不建集；error-rate-percent 默认 100）。 */
    public record Policy(String dataset, int errorRatePercent, int minInputChars) {

        public Policy {
            if (dataset == null || dataset.isBlank()) {
                throw new IllegalArgumentException("dataset 非空");
            }
            if (errorRatePercent < 0 || errorRatePercent > 100) {
                throw new IllegalArgumentException("error-rate-percent 取值 [0,100]：" + errorRatePercent);
            }
            minInputChars = Math.max(0, minInputChars);
        }
    }

    private final EvalDatasetStore datasetStore;
    private final Policy policy;
    private final String sessionId;
    /** 当前轮输入（onTurnStart 记 / onTurnEnd|onTurnError 清——信号串行化无锁安全）。 */
    private String currentInput;

    public TurnErrorSampler(EvalDatasetStore datasetStore, Policy policy, String sessionId) {
        this.datasetStore = datasetStore;
        this.policy = policy;
        this.sessionId = sessionId;
    }

    @Override
    public void onTurnStart(int turnSeq, String userInput) {
        this.currentInput = userInput;
    }

    @Override
    public void onTurnEnd(int turnSeq, String finalReply) {
        this.currentInput = null; // 成功收尾——错误采样清场
    }

    @Override
    public void onTurnError(int turnSeq, Throwable error) {
        String input = this.currentInput;
        this.currentInput = null;
        if (input == null || input.isBlank() || input.length() < policy.minInputChars()) {
            return; // 缺输入/短问过滤
        }
        if (!sampled(turnSeq)) {
            return;
        }
        try {
            datasetStore.addItem(policy.dataset(), input, placeholder(error), sessionId, turnSeq);
            BuzhouMetricsHolder.metrics().counter("buzhou.eval.error-sampled");
        } catch (RuntimeException e) {
            // fail-soft：数据集未建等宿主漏配——计数暴露，绝不炸轮
            BuzhouMetricsHolder.metrics().counter("buzhou.eval.error-sampling-failed");
        }
    }

    /** 确定性采样判定（与 407 同公式——同轮同判，重放不改变决定）。 */
    boolean sampled(int turnSeq) {
        if (policy.errorRatePercent() >= 100) {
            return true;
        }
        if (policy.errorRatePercent() <= 0) {
            return false;
        }
        int bucket = Math.floorMod((sessionId + ":" + turnSeq).hashCode(), 100);
        return bucket < policy.errorRatePercent();
    }

    /** 错误摘要占位（异常类+截断消息——golden 判断留人工）。 */
    static String placeholder(Throwable error) {
        String message = error.getMessage() == null ? "" : error.getMessage();
        if (message.length() > MAX_MESSAGE_CHARS) {
            message = message.substring(0, MAX_MESSAGE_CHARS);
        }
        return PLACEHOLDER_PREFIX + error.getClass().getSimpleName() + ": " + message;
    }
}
