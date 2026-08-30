package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 177 / T548：出站脱敏回归——内置型脱成占位符 / 自定义叠加 / 非串原样 /
 * 无命中零改写 / 装饰下发与参数校验。
 */
class PiiEventRedactorTest {

    private static final class Collector implements
            io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener {
        final CopyOnWriteArrayList<SessionEvent> received = new CopyOnWriteArrayList<>();

        @Override
        public void onEvent(SessionEvent event) {
            received.add(event);
        }
    }

    @Test
    void builtinTypesRedactedInPayloadStrings() {
        Collector sink = new Collector();
        PiiEventRedactor redactor = new PiiEventRedactor(sink);

        redactor.onEvent(SessionEvent.of("user.turn.completed",
                Map.of("input", "帮我查 13812345678", "sessionId", "s1")));

        SessionEvent out = sink.received.getFirst();
        assertThat(out.type()).isEqualTo("user.turn.completed");
        assertThat((String) out.payload().get("input")).isEqualTo("帮我查 [PII:CN_PHONE]");
        assertThat((String) out.payload().get("sessionId")).isEqualTo("s1"); // 无命中
    }

    @Test
    void customRulesStackOnBuiltin() {
        Collector sink = new Collector();
        PiiEventRedactor redactor = new PiiEventRedactor(sink, null, new CustomPiiRules(
                List.of(CustomPiiRules.Rule.of("ORDER_ID", "ORD-\\d{6,}"))));

        redactor.onEvent(SessionEvent.of("order.viewed",
                Map.of("detail", "订单 ORD-20260001 联系 a@b.co")));

        assertThat((String) sink.received.getFirst().payload().get("detail"))
                .isEqualTo("订单 [PII:ORDER_ID] 联系 [PII:EMAIL]");
    }

    @Test
    void nonStringValuesPassThroughUntouched() {
        Collector sink = new Collector();
        PiiEventRedactor redactor = new PiiEventRedactor(sink);
        Map<String, Object> payload = Map.of("count", 42, "ok", true, "nested", Map.of("a", 1));

        redactor.onEvent(SessionEvent.of("metrics", payload));

        SessionEvent out = sink.received.getFirst();
        assertThat(out.payload().get("count")).isEqualTo(42);
        assertThat(out.payload().get("ok")).isEqualTo(Boolean.TRUE);
        assertThat(out.payload().get("nested")).isSameAs(payload.get("nested")); // 非 String 同引用
    }

    @Test
    void cleanTextKeepsSameReference() {
        Collector sink = new Collector();
        PiiEventRedactor redactor = new PiiEventRedactor(sink);
        String clean = "正常遥测文本";

        redactor.onEvent(SessionEvent.of("t", Map.of("msg", clean)));

        assertThat(sink.received.getFirst().payload().get("msg")).isSameAs(clean);
    }

    @Test
    void delegateRequiredAndEventShapePreserved() {
        assertThatThrownBy(() -> new PiiEventRedactor(null))
                .isInstanceOf(IllegalArgumentException.class);

        Collector sink = new Collector();
        SessionEvent original = SessionEvent.of("t", Map.of("x", "y"));
        new PiiEventRedactor(sink).onEvent(original);

        SessionEvent out = sink.received.getFirst();
        assertThat(out.occurredAt()).isEqualTo(original.occurredAt()); // 时间与类型保形
        assertThat(out.payload()).isEqualTo(original.payload());
    }
}
