package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1211 / impl 876：PII 出站脱敏读面——脱敏改写（redacted）、无命中透传
 * （cleanPassthrough）、fail-open 三结局、守恒恒等式、resetForTest 归零。
 */
class PiiEventRedStatsTest {

    private final AtomicInteger forwarded = new AtomicInteger();
    private SessionEvent lastForwarded;

    private PiiEventRedactor redactor;

    @BeforeEach
    void reset() {
        PiiEventRedactor.resetForTest();
        forwarded.set(0);
        lastForwarded = null;
        PiiEventRedactor r = new PiiEventRedactor(event -> {
            forwarded.incrementAndGet();
            lastForwarded = event;
        });
        redactor = r;
    }

    private SessionEvent event(String payloadValue) {
        return new SessionEvent("test.type", Map.of("value", payloadValue), Instant.now());
    }

    @Test
    void redactedEventCountsRedacted() {
        redactor.onEvent(event("邮箱 test@example.com 泄露"));

        PiiEventRedactor.PiiEventRedStats stats = PiiEventRedactor.stats();
        assertThat(stats.eventsProcessed()).isEqualTo(1);
        assertThat(stats.redacted()).isEqualTo(1);
        assertThat(stats.cleanPassthrough()).isZero();
        assertThat(stats.failOpen()).isZero();
    }

    @Test
    void cleanEventCountsCleanPassthrough() {
        redactor.onEvent(event("完全干净的内容"));

        PiiEventRedactor.PiiEventRedStats stats = PiiEventRedactor.stats();
        assertThat(stats.cleanPassthrough()).isEqualTo(1);
        assertThat(stats.redacted()).isZero();
    }

    @Test
    void conservationIdentityHoldsAcrossMixedEvents() {
        redactor.onEvent(event("邮箱 test@example.com 泄露")); // redacted
        redactor.onEvent(event("干净内容"));                    // cleanPassthrough
        redactor.onEvent(event("另一段干净文本"));              // cleanPassthrough

        PiiEventRedactor.PiiEventRedStats stats = PiiEventRedactor.stats();
        assertThat(stats.eventsProcessed()).isEqualTo(3);
        assertThat(stats.eventsProcessed())
                .isEqualTo(stats.redacted() + stats.cleanPassthrough() + stats.failOpen());
        assertThat(stats.redacted()).isEqualTo(1);
        assertThat(stats.cleanPassthrough()).isEqualTo(2);
    }

    @Test
    void resetForTestZeroesCounters() {
        redactor.onEvent(event("邮箱 test@example.com"));
        assertThat(PiiEventRedactor.stats().eventsProcessed()).isEqualTo(1);

        PiiEventRedactor.resetForTest();

        PiiEventRedactor.PiiEventRedStats stats = PiiEventRedactor.stats();
        assertThat(stats.eventsProcessed()).isZero();
        assertThat(stats.redacted()).isZero();
    }
}
