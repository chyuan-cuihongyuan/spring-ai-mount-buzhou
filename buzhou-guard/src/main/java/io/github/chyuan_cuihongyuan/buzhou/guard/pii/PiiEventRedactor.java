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

    @Override
    public void onEvent(SessionEvent event) {
        delegate.onEvent(new SessionEvent(event.type(), redactPayload(event.payload()),
                event.occurredAt()));
    }

    /** 一层脱敏：String 值过检测器+自定义；非 String 原样；无命中同引用（嵌套递归留档）。 */
    private Map<String, Object> redactPayload(Map<String, Object> payload) {
        Map<String, Object> out = new LinkedHashMap<>();
        payload.forEach((key, value) -> {
            if (value instanceof String text) {
                out.put(key, redactText(text));
            } else {
                out.put(key, value); // 数字/布尔/空值天然安全
            }
        });
        return out;
    }

    private String redactText(String text) {
        try {
            String redacted = detector.redact(text, enabledTypes);
            if (!customRules.isEmpty()) {
                redacted = customRules.redact(redacted);
            }
            return redacted == null ? text : redacted;
        } catch (RuntimeException e) {
            return text; // fail-open：脱敏失败不阻断出站
        }
    }
}
