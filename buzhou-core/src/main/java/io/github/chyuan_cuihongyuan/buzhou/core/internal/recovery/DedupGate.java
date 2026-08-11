package io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.IdempotencyKeyExtractor;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.IdempotencyKeys;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.springframework.ai.chat.model.ToolContext;

import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 幂等去重闸门（spec「幂等三件套」）：执行脊柱与恢复重放共用的去重决策点。
 *
 * <p>聚合 {@link DedupRecorder}（存储操作）+ 键提取器表（业务覆盖键）+ 事件发射器，
 * 供 {@code HarnessToolCallingManager.executeOne}（live 路径）与 {@code DanglingCallRepairer}
 * （恢复重放路径）复用同一套键派生与命中语义。
 *
 * <p>键派生优先级：工具声明 {@link IdempotencyKeyExtractor} 且提取出非空业务键 → 业务覆盖键；
 * 否则 → 框架默认键（会话 + 轮次 + 调用序号，即 toolCallId）。
 */
public final class DedupGate {

    private final DedupRecorder recorder;
    private final Map<String, IdempotencyKeyExtractor> extractorsByName;
    private final Consumer<SessionEvent> eventEmitter;

    public DedupGate(DedupRecorder recorder,
                     Map<String, IdempotencyKeyExtractor> extractorsByName,
                     Consumer<SessionEvent> eventEmitter) {
        this.recorder = recorder;
        this.extractorsByName = extractorsByName == null ? Map.of() : Map.copyOf(extractorsByName);
        this.eventEmitter = eventEmitter == null ? event -> {
        } : eventEmitter;
    }

    public DedupRecorder recorder() {
        return recorder;
    }

    /**
     * 派生本次工具调用的幂等键。
     *
     * @param toolName   工具名
     * @param toolCallId 模型下发的工具调用 id
     * @param arguments  工具入参（JSON 串，供业务键提取）
     * @param toolContext 工具上下文（恢复重放路径可为 {@code null}）
     * @return 幂等键（业务覆盖优先，缺省走框架默认键）
     */
    public String keyOf(String toolName, String toolCallId, String arguments, ToolContext toolContext) {
        IdempotencyKeyExtractor extractor = extractorsByName.get(toolName);
        if (extractor != null) {
            String businessKey = extractor.extractKey(arguments, toolContext);
            if (businessKey != null && !businessKey.isBlank()) {
                return IdempotencyKeys.businessKey(toolName, businessKey);
            }
        }
        return IdempotencyKeys.defaultKey(toolName, toolCallId);
    }

    /** 去重命中进既有事件通道（键 + 工具名，SRE 可见哪些工具走了去重记录而非真实执行）。 */
    public void emitHit(String toolName, String key) {
        eventEmitter.accept(new SessionEvent("dedup-hit",
                Map.of("toolName", toolName, "key", key), Instant.now()));
    }
}
