package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import java.io.Writer;
import java.util.List;

/**
 * 死信 JSONL 导出（spec 542 / T827，60/67 导出族同构）：死信清单一行一
 * JSON（eventId/type/attempts/createdAt）——与 OLAP/归档管道同构搬运。
 * 纯函数式导出（源经 forwarder.deadLetters() 查询）。
 */
public final class WebhookDeadLetterJsonl {

    private WebhookDeadLetterJsonl() {
    }

    /** 导出（返回行数）。 */
    public static long export(Writer out, List<WebhookDeadLetter> deadLetters) throws java.io.IOException {
        long lines = 0;
        for (WebhookDeadLetter letter : deadLetters) {
            out.write("{\"eventId\":\"" + escape(letter.eventId())
                    + "\",\"type\":\"" + escape(letter.type())
                    + "\",\"attempts\":" + letter.attempts()
                    + ",\"createdAtEpochMs\":" + letter.createdAt().toEpochMilli() + "}\n");
            lines++;
        }
        return lines;
    }

    private static String escape(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
