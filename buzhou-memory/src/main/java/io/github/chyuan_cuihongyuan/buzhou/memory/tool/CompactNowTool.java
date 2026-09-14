package io.github.chyuan_cuihongyuan.buzhou.memory.tool;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.ManualCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.BiTemporalFactLedger;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryFactReconciler;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@code compact_now}：语义边界压缩触发工具（wayfinder T27 / docs/spec/11 memory，来源
 * LangChain Deep Agents）。模型可在任务边界/长草稿前<b>自触发压缩</b>——把未摘要的完成轮
 * 折入九段摘要；token 阈值兜底仍照常生效（<b>双触发路径</b>：质量自触发 + token 安全网）。
 *
 * <p>impl-65 / spec 20：压缩管线抽取到 {@link ManualCompactor}（宿主侧手动压缩共用同一条
 * 管线），本工具只做会话绑定解析与文本回报。
 *
 * <p>会话绑定：{@code HarnessToolCallingManager} 注入的 ToolContext 携带 sessionId；
 * 幂等安全：复用 {@code coversUpToTurn} 轮次水位与 {@code summarizedMessageIds} 消息水位
 * （T24），已摘要内容不重复折入。
 */
public class CompactNowTool implements ToolCallback {

    // —— spec 1059 / impl 811：手动压缩判定读面（Anthropic /compact 采用率思想；
    // 静态面理由同 R46–R58 先例）。守恒：calls = 四结局桶之和。
    private static final AtomicLong CALLS = new AtomicLong();
    private static final AtomicLong SUCCESSES = new AtomicLong();
    private static final AtomicLong SKIPPEDS = new AtomicLong();
    private static final AtomicLong FAILURES = new AtomicLong();
    private static final AtomicLong UNBOUND_REJECTS = new AtomicLong();

    /** 手动压缩判定分布快照（spec 1059）。 */
    public record CompactNowStats(long calls, long successes, long skippeds,
                                  long failures, long unboundRejects) {
    }

    /** 只读快照（守恒 calls = successes + skippeds + failures + unboundRejects）。 */
    public static CompactNowStats stats() {
        return new CompactNowStats(CALLS.get(), SUCCESSES.get(), SKIPPEDS.get(),
                FAILURES.get(), UNBOUND_REJECTS.get());
    }

    /** 测试专用归零（生产禁用——计数器是进程生命周期水位）。 */
    public static void resetForTest() {
        CALLS.set(0);
        SUCCESSES.set(0);
        SKIPPEDS.set(0);
        FAILURES.set(0);
        UNBOUND_REJECTS.set(0);
    }

    private final ManualCompactor compactor;
    private final int keepRecentTurns;

    public CompactNowTool(MessageStore messageStore, SummaryStoreBridge summaryBridge,
                          SummaryGenerator summaryGenerator, ChatModel summaryModel,
                          int keepRecentTurns) {
        this(messageStore, summaryBridge, summaryGenerator, summaryModel, keepRecentTurns,
                null, null, null);
    }

    public CompactNowTool(MessageStore messageStore, SummaryStoreBridge summaryBridge,
                          SummaryGenerator summaryGenerator, ChatModel summaryModel,
                          int keepRecentTurns, SummaryFactReconciler reconciler,
                          BiTemporalFactLedger biTemporal,
                          java.util.function.Consumer<SessionEvent> eventSink) {
        this.compactor = new ManualCompactor(messageStore, summaryBridge, summaryGenerator,
                summaryModel, keepRecentTurns, reconciler, biTemporal, eventSink);
        this.keepRecentTurns = Math.max(0, keepRecentTurns);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("compact_now")
                .description("在任务边界或长草稿前主动触发会话压缩：把已完成的早前轮次折入九段式结构化摘要，"
                        + "为后续推理腾出上下文预算（保 P0 关键事实，压缩发生在干净边界、保真度更高）。"
                        + "可选传 reason 说明触发时机。")
                .inputSchema("{\"type\":\"object\",\"properties\":{\"reason\":{\"type\":\"string\"}},"
                        + "\"additionalProperties\":false}")
                .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        CALLS.incrementAndGet();
        String sessionId = HarnessToolCallingManager.sessionIdOf(toolContext);
        if (sessionId == null || sessionId.isBlank()) {
            UNBOUND_REJECTS.incrementAndGet();
            return "[compact_now] 当前未绑定会话，无法定位待压缩历史。";
        }
        ManualCompactor.CompactResult result = compactor.compact(sessionId);
        if (result.skipped()) {
            SKIPPEDS.incrementAndGet();
            return "[compact_now] 无需压缩：待折入消息为空（已全部折入摘要或会话历史为空）。";
        }
        if (result.error() != null) {
            FAILURES.incrementAndGet();
            return "[compact_now] 压缩失败：" + result.error()
                    + "。token 预算兜底压缩仍会在需要时自动触发。";
        }
        SUCCESSES.incrementAndGet();
        return "[compact_now] 压缩完成：新折入 " + result.foldedMessages() + " 条消息（第 "
                + result.fromTurn() + "–" + result.toTurn() + " 轮），摘要代际 "
                + result.generation() + "，估算 " + result.estimatedTokens()
                + " token。近期 " + keepRecentTurns + " 轮原文保持内联；下一轮注入视图将携带最新摘要。";
    }
}
