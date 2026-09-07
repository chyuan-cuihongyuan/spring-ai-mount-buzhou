package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import org.springframework.ai.chat.client.ChatClientResponse;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 重复检测 hook（spec 326 / T643）：order 60（心跳 50 后）——afterModel 喂
 * 响应文本给 per-session {@link TurnRepetitionDetector}；fire 计数；opt-in
 * {@code unstick} 时 fire 当次调用 block 回填「[重复检测]」解困指令（替换
 * 复读输出——行为变化故默认 observe-only）。检测器超 1024 会话整体重置
 * （诚实降级）。
 */
public final class RepetitionDetectorHook implements BuzhouHook {

    public static final int ORDER = 60;
    static final String MARKER = "[重复检测]";
    static final int MAX_SESSIONS = 1024;

    private final int window;
    private final double similarityPercent;
    private final boolean unstick;
    private final Map<String, TurnRepetitionDetector> detectors = new ConcurrentHashMap<>();

    public RepetitionDetectorHook(int window, double similarityPercent, boolean unstick) {
        this.window = window;
        this.similarityPercent = similarityPercent;
        this.unstick = unstick;
    }

    @Override
    public String name() {
        return "RepetitionDetectorHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult afterModel(ModelCallContext ctx) {
        if (ctx == null || ctx.sessionId() == null) {
            return HookResult.CONTINUE;
        }
        Optional<TurnRepetitionDetector.Verdict> verdict =
                detectorFor(ctx.sessionId()).record(textOf(ctx.response()));
        if (verdict.isPresent() && unstick) {
            return HookResult.block(MARKER + "\n检测到连续 " + verdict.get().runLength()
                    + " 次近乎相同的输出（相似度 "
                    + String.format("%.0f%%", verdict.get().similarity() * 100)
                    + "）——你在打转。请立刻换策略：改用其他工具/换个角度回答/"
                    + "直接给出当前结论收束任务。");
        }
        return HookResult.CONTINUE;
    }

    /** 会话当前 run 长（观测面/测试）。 */
    public int currentRun(String sessionId) {
        TurnRepetitionDetector detector = detectors.get(sessionId);
        return detector == null ? 0 : detector.currentRun();
    }

    private TurnRepetitionDetector detectorFor(String sessionId) {
        TurnRepetitionDetector detector = detectors.get(sessionId);
        if (detector != null) {
            return detector;
        }
        if (detectors.size() >= MAX_SESSIONS) {
            detectors.clear(); // 诚实降级：超上限整体重置（run 重新累计）
        }
        return detectors.computeIfAbsent(sessionId,
                k -> new TurnRepetitionDetector(window, similarityPercent));
    }

    /** 主链路文本抽取（eval 族同款），逐层 null 防御。 */
    private static String textOf(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null
                || response.chatResponse().getResult() == null
                || response.chatResponse().getResult().getOutput() == null) {
            return "";
        }
        return response.chatResponse().getResult().getOutput().getText();
    }
}
