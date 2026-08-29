package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 209 / T584：schema 检查回归——缺键拦计 / 全键过 / 未声明放行 /
 * failOpen 观察模式 / 空键集 / 透传保真。
 */
class EventSchemaCheckerTest {

    private static final class Collector implements SessionEventListener {
        final CopyOnWriteArrayList<SessionEvent> received = new CopyOnWriteArrayList<>();

        @Override
        public void onEvent(SessionEvent event) {
            received.add(event);
        }
    }

    @Test
    void missingRequiredKeysAreDropped() {
        Collector sink = new Collector();
        EventSchemaChecker checker = new EventSchemaChecker(sink,
                Map.of("user.turn.completed", Set.of("sessionId", "turnSeq")));

        checker.onEvent(SessionEvent.of("user.turn.completed",
                Map.of("sessionId", "s1"))); // 缺 turnSeq

        assertThat(sink.received).isEmpty(); // fail-closed 不出门
        assertThat(checker.violations(SessionEvent.of("user.turn.completed",
                Map.of("sessionId", "s1")))).containsExactly("turnSeq");
    }

    @Test
    void fullyPopulatedEventPassesUntouched() {
        Collector sink = new Collector();
        EventSchemaChecker checker = new EventSchemaChecker(sink,
                Map.of("user.turn.completed", Set.of("sessionId", "turnSeq")));
        SessionEvent complete = SessionEvent.of("user.turn.completed",
                Map.of("sessionId", "s1", "turnSeq", 3, "extra", "无害多键"));

        checker.onEvent(complete);

        assertThat(sink.received).hasSize(1);
        assertThat(sink.received.getFirst().payload()).containsEntry("turnSeq", 3); // 透传保真
    }

    @Test
    void undeclaredTypesPassThroughOpenWorld() {
        Collector sink = new Collector();
        EventSchemaChecker checker = new EventSchemaChecker(sink,
                Map.of("known.type", Set.of("a")));

        checker.onEvent(SessionEvent.of("brand.new.type", Map.of("whatever", 1)));

        assertThat(sink.received).hasSize(1); // 未声明不被卡死
        assertThat(checker.violations(SessionEvent.of("x", Map.of()))).isEmpty();
    }

    @Test
    void failOpenModeObservesWithoutBlocking() {
        Collector sink = new Collector();
        EventSchemaChecker checker = new EventSchemaChecker(sink,
                Map.of("t", Set.of("mustHave")), true); // 观察模式

        checker.onEvent(SessionEvent.of("t", Map.of("other", 1)));

        assertThat(sink.received).hasSize(1); // 违规可见但不拦（调查期）
        assertThat(checker.violations(sink.received.getFirst()))
                .containsExactly("mustHave");
    }

    @Test
    void emptyRequiredSetOnlyChecksExistence() {
        Collector sink = new Collector();
        EventSchemaChecker checker = new EventSchemaChecker(sink,
                Map.of("any.payload", Set.of()));

        checker.onEvent(SessionEvent.of("any.payload", Map.of())); // 空 payload 也过
        assertThat(sink.received).hasSize(1);
    }

    @Test
    void delegateRequired() {
        assertThatThrownBy(() -> new EventSchemaChecker(null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
