package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 542 / T827：死信 JSONL 导出——一行一死信（eventId/type/attempts/
 * createdAt epoch）、转义完备、行数返回。
 */
class WebhookDeadLetterJsonlTest {

    @Test
    void exportsOneLinePerDeadLetter() throws Exception {
        StringWriter out = new StringWriter();
        long lines = WebhookDeadLetterJsonl.export(out, List.of(
                new WebhookDeadLetter("evt-1", "user.turn.completed", 5, Instant.EPOCH),
                new WebhookDeadLetter("evt\"2\"", "sess\nclosed", 2, Instant.ofEpochMilli(1000))));
        assertThat(lines).isEqualTo(2);
        String json = out.toString();
        assertThat(json).contains("\"eventId\":\"evt-1\"");
        assertThat(json).contains("evt\\\"2\\\""); // 引号转义（内嵌引号 → \"）
        assertThat(json).contains("sess\\nclosed"); // 换行转义——单行 JSONL
        assertThat(json).contains("\"attempts\":2");
    }

    @Test
    void emptyListExportsZeroLines() throws Exception {
        StringWriter out = new StringWriter();
        assertThat(WebhookDeadLetterJsonl.export(out, List.of())).isZero();
        assertThat(out.toString()).isEmpty();
    }
}
