package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.Spotlighting;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MemoryViewProcessor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * hot-tail / cold-storage 两级保留（wayfinder T21 / docs/spec/11 spill，来源 Claude Code
 * microcompaction）：近期 N 条工具结果<b>全量内联</b>（供推理，零损失），更旧的 TOOL 消息
 * 超阈值时溢出至 SpillStore、视图内替换为 T20 自描述占位符——推理所需的近期数据零损失、
 * 旧数据不占窗口。
 *
 * <p>这是<b>视图级</b>处理器（注入视图构建时惰性溢出，存储层仍 append-only）；与
 * {@code SpillOffloadHook} 的 afterTool 即时溢出<b>互斥使用</b>：启用 hot-tail 模式时
 * 应关闭即时 offload（{@code offloadEnabled(false)}），否则大结果在产生时即被替换，
 * hot-tail 无从保留近期全量内联。
 *
 * <p>durable 覆盖（T22）：工具策略声明 {@code spillNeverOffload: true} 的输出永不溢出
 * （对截断敏感的 DB schema、整文件等）；{@code spillThresholdTokens}/{@code spillThresholdChars}
 * 按工具覆盖「超 X 才溢出」。
 */
public class HotTailViewProcessor implements MemoryViewProcessor {

    /** 视图级溢出在 SpillUri agent 段使用的固定标识（视图处理器拿不到 agentName）。 */
    public static final String VIEW_AGENT = "hot-tail";

    private final SpillService spillService;
    private final int keepInlineToolResults;
    private final long maxInlineChars;
    private final Function<SpillUri, Path> spillFileResolver;
    private final SessionReadOnlyRegistry readOnlyRegistry;
    private final Map<String, Object> toolPolicies;
    private final int defaultThresholdChars;
    /** impl-16 / T44：句柄生命周期（显式逐出 + TTL 自动）；null = 不启用 context-clearing。 */
    private HandleLifecycleRegistry handleLifecycle;
    private int handleTtlTurns = 3;

    public void setHandleLifecycle(HandleLifecycleRegistry handleLifecycle, int ttlTurns) {
        this.handleLifecycle = handleLifecycle;
        this.handleTtlTurns = Math.max(1, ttlTurns);
    }

    public HotTailViewProcessor(SpillService spillService,
                                int keepInlineToolResults,
                                long maxInlineChars,
                                Function<SpillUri, Path> spillFileResolver,
                                SessionReadOnlyRegistry readOnlyRegistry,
                                Map<String, Object> toolPolicies,
                                int defaultThresholdChars) {
        this.spillService = spillService;
        this.keepInlineToolResults = Math.max(1, keepInlineToolResults);
        this.maxInlineChars = maxInlineChars;
        this.spillFileResolver = spillFileResolver;
        this.readOnlyRegistry = readOnlyRegistry;
        this.toolPolicies = toolPolicies == null ? Map.of() : toolPolicies;
        this.defaultThresholdChars = defaultThresholdChars <= 0
                ? SpillOffloadHook.DEFAULT_THRESHOLD_CHARS : defaultThresholdChars;
    }

    @Override
    public List<BuzhouMessage> process(String sessionId, List<BuzhouMessage> stored, int currentTurn) {
        List<Integer> toolIndexes = new ArrayList<>();
        for (int i = 0; i < stored.size(); i++) {
            if (stored.get(i).role() == Role.TOOL) {
                toolIndexes.add(i);
            }
        }
        if (toolIndexes.size() <= keepInlineToolResults && maxInlineChars <= 0) {
            return stored; // 全部属 hot-tail 且无大小预算，零处理
        }
        // hot-tail = 最近 N 条 TOOL 消息（全量内联）；其余为 cold 候选（钳位：条数不足 N 时无 cold）
        int coldCount = Math.max(0, toolIndexes.size() - keepInlineToolResults);
        List<Integer> coldIndexes = toolIndexes.subList(0, coldCount);

        List<BuzhouMessage> result = new ArrayList<>(stored);
        for (int idx : coldIndexes) {
            if (trySpillAt(result, idx, sessionId)) {
                continue;
            }
        }

        // 大小预算护栏：内联 TOOL 内容总字符超 maxInlineChars（>0 时启用）则从最旧开始补溢出
        if (maxInlineChars > 0) {
            spillBeyondBudget(sessionId, result, toolIndexes);
        }
        // impl-16 / T44：context-clearing——已消费句柄（显式逐出 / TTL 过期）收缩为极简墓碑；
        // 整窗一次性批量处理（视图级幂等重建，避免每 Turn 增量改写触发 cache 断点失效）
        if (handleLifecycle != null) {
            handleLifecycle.absorbReads(currentTurn);
            tombstoneEvictedHandles(result, currentTurn);
        }
        return result;
    }

    /** 已逐出句柄的占位符 → 极简墓碑（比自描述占位符更省；原文在 SpillStore 随时可回读）。 */
    private void tombstoneEvictedHandles(List<BuzhouMessage> result, int currentTurn) {
        for (int i = 0; i < result.size(); i++) {
            BuzhouMessage message = result.get(i);
            if (message.role() != Role.TOOL || message.content() == null) {
                continue;
            }
            String content = message.content();
            int uriAt = content.indexOf("spill://");
            if (uriAt < 0) {
                continue;
            }
            // 提取首个 spill:// URI（占位符形状固定；逐出判定按 URI）
            int end = uriAt;
            while (end < content.length() && !Character.isWhitespace(content.charAt(end))
                    && content.charAt(end) != ']' && content.charAt(end) != '）') {
                end++;
            }
            String uri = content.substring(uriAt, end);
            if (handleLifecycle.isEvicted(uri, currentTurn, handleTtlTurns)) {
                result.set(i, withContent(message, "[句柄已逐出：" + uri + "；原文可随时回读]"));
            }
        }
    }

    private void spillBeyondBudget(String sessionId, List<BuzhouMessage> result, List<Integer> toolIndexes) {
        for (int idx : toolIndexes) {
            if (inlineToolChars(result, toolIndexes) <= maxInlineChars) {
                return;
            }
            trySpillAt(result, idx, sessionId);
        }
    }

    /** 当前视图内 TOOL 内容总字符数（每轮重算——占位符可能大于原文，增量记账会失真）。 */
    private long inlineToolChars(List<BuzhouMessage> result, List<Integer> toolIndexes) {
        long total = 0;
        for (int idx : toolIndexes) {
            String content = result.get(idx).content();
            total += content == null ? 0 : content.length();
        }
        return total;
    }

    /**
     * 尝试溢出指定位置的工具消息（幂等/解包裹/durable/阈值判定集中于此）：
     * spotlight 包裹的历史消息先还原（SpillStore 存干净原文）；已是占位符的跳过；
     * 溢出成功替换为 T20 自描述占位符；降级透传（onFail=FILTER 既有语义）。
     */
    private boolean trySpillAt(List<BuzhouMessage> result, int idx, String sessionId) {
        BuzhouMessage message = result.get(idx);
        String toolName = String.valueOf(message.metadata().getOrDefault("toolName", ""));
        if (SpillThresholds.isDurable(toolPolicies, toolName)) {
            return false; // 「永不溢出」声明（T22）
        }
        String raw = message.content() == null ? "" : message.content();
        String effective = Spotlighting.unwrap(raw);
        if (effective.contains("spill://") || effective.contains("[旧工具结果已清理")) {
            return false; // 已是占位符/已处理（幂等）
        }
        int threshold = SpillThresholds.thresholdFor(toolPolicies, toolName, defaultThresholdChars);
        if (effective.length() < threshold) {
            return false; // 未超「超 X 才溢出」阈值
        }
        String callId = message.toolCallId() == null ? message.id() : message.toolCallId();
        SpillService.OffloadOutcome outcome = spillService.tryOffload(
                VIEW_AGENT, sessionId, callId, toolName, effective, threshold);
        if (outcome.offloaded()) {
            registerReadOnly(sessionId, outcome.uri());
            // impl-16 / T44：句柄登记（TTL 从溢出轮起算；回读刷新引用）
            if (handleLifecycle != null) {
                handleLifecycle.track(outcome.uri().toString(), currentTurnOf(result));
            }
            result.set(idx, withContent(message, outcome.text()));
            return true;
        }
        return false; // degraded → 降级透传（onFail=FILTER）
    }

    /** 从视图推断当前轮次（取末条消息 turnSeq；回退 0）。 */
    private static int currentTurnOf(List<BuzhouMessage> view) {
        for (int i = view.size() - 1; i >= 0; i--) {
            return view.get(i).turnSeq();
        }
        return 0;
    }

    private void registerReadOnly(String sessionId, SpillUri uri) {
        try {
            Path file = spillFileResolver.apply(uri);
            if (file != null) {
                readOnlyRegistry.register(sessionId, file);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private BuzhouMessage withContent(BuzhouMessage message, String content) {
        return new BuzhouMessage(message.id(), message.sessionId(), message.turnSeq(), message.seqInTurn(),
                message.role(), content, message.toolCalls(), message.toolCallId(),
                message.reasoningContent(), message.reasoningSignature(), message.metadata(),
                message.createdAt());
    }
}
