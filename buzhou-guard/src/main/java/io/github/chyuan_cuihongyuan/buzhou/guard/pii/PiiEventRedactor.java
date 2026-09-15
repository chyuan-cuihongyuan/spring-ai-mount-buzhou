package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 事件载荷出站脱敏装饰器（spec 177 / T547，PII 防线出站面收口）：onEvent →
 * payload 中每个 String 值过 {@link PiiDetector}（内置五型）+
 * {@link CustomPiiRules}（叠加，宿主实例可复用），构造脱敏后新事件下发被装饰
 * listener；非 String 值原样、无命中 String 同引用零改写、脱敏异常 fail-open
 * 原文下发（出站可用性优先）。与 webhook fanout（spec 151）组合即全站出站脱敏。
 */
public final class PiiEventRedactor implements SessionEventListener {

    private final SessionEventListener delegate;
    private final PiiDetector detector = new PiiDetector();
    private final Set<PiiType> enabledTypes;
    private final CustomPiiRules customRules;

    public PiiEventRedactor(SessionEventListener delegate) {
        this(delegate, null, null);
    }

    public PiiEventRedactor(SessionEventListener delegate, Set<PiiType> enabledTypes,
                            CustomPiiRules customRules) {
        if (delegate == null) {
            throw new IllegalArgumentException("被装饰 listener 非空");
        }
        this.delegate = delegate;
        this.enabledTypes = enabledTypes == null || enabledTypes.isEmpty()
                ? EnumSet.allOf(PiiType.class) : EnumSet.copyOf(enabledTypes);
        this.customRules = customRules == null ? new CustomPiiRules(List.of()) : customRules;
    }

    // —— spec 1211 / impl 876：出站脱敏读面（出站网关覆盖率思想；静态面理由同
    // R46–R121 先例）。守恒：eventsProcessed = 三结局桶之和。
    private static final java.util.concurrent.atomic.AtomicLong EVENTS_PROCESSED =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong REDACTED =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong CLEAN_PASSTHROUGH =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong FAIL_OPEN =
            new java.util.concurrent.atomic.AtomicLong();

    /** 出站脱敏分布快照（spec 1211）。 */
    public record PiiEventRedStats(long eventsProcessed, long redacted,
                                   long cleanPassthrough, long failOpen) {
    }

    /** 只读快照（守恒 eventsProcessed = redacted + cleanPassthrough + failOpen）。 */
    public static PiiEventRedStats stats() {
        return new PiiEventRedStats(EVENTS_PROCESSED.get(), REDACTED.get(),
                CLEAN_PASSTHROUGH.get(), FAIL_OPEN.get());
    }

    /** 测试专用归零（生产禁用——计数器是进程生命周期水位）。 */
    public static void resetForTest() {
        EVENTS_PROCESSED.set(0);
        REDACTED.set(0);
        CLEAN_PASSTHROUGH.set(0);
        FAIL_OPEN.set(0);
    }

    @Override
    public void onEvent(SessionEvent event) {
        EVENTS_PROCESSED.incrementAndGet();
        delegate.onEvent(new SessionEvent(event.type(), redactPayload(event.payload()),
                event.occurredAt()));
    }

    /** 一层脱敏：String 值过检测器+自定义；非 String 原样；无命中同引用（嵌套递归留档）。 */
    private Map<String, Object> redactPayload(Map<String, Object> payload) {
        boolean anyRedacted = false;
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : payload.entrySet()) {
            if (e.getValue() instanceof String text) {
                String after = redactText(text);
                if (after != text) {
                    anyRedacted = true;
                }
                out.put(e.getKey(), after);
            } else {
                out.put(e.getKey(), e.getValue()); // 数字/布尔/空值天然安全
            }
        }
        if (anyRedacted) {
            REDACTED.incrementAndGet();
        } else {
            CLEAN_PASSTHROUGH.incrementAndGet();
        }
        return out;
    }

    private String redactText(String text) {
        try {
            String redacted = detector.redact(text, enabledTypes);
            if (!customRules.isEmpty()) {
                redacted = customRules.redact(redacted);
            }
            if (redacted != text) {
                REDACTED.incrementAndGet();
                return redacted;
            }
            CLEAN_PASSTHROUGH.incrementAndGet();
            return text;
        } catch (RuntimeException e) {
            FAIL_OPEN.incrementAndGet();
            return text; // fail-open：脱敏失败不阻断出站
        }
    }
}
