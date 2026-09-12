package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 死信 JSONL 导出（spec 542 / T827，60/67 导出族同构）：死信清单一行一
 * JSON（eventId/type/attempts/createdAt）——与 OLAP/归档管道同构搬运。
 * 纯函数式导出（源经 forwarder.deadLetters() 查询）。
 */
public final class WebhookDeadLetterJsonl {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private WebhookDeadLetterJsonl() {
    }

    /**
     * 导出（返回行数）。spec 644 / T938：行序列化走 Jackson（spec 60 纪律——
     * 任意字符合法转义，含旧自有 escape 不覆盖的控制字符）。
     */
    public static long export(Writer out, List<WebhookDeadLetter> deadLetters) throws java.io.IOException {
        long lines = 0;
        for (WebhookDeadLetter letter : deadLetters) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("eventId", letter.eventId());
            row.put("type", letter.type());
            row.put("attempts", letter.attempts());
            row.put("createdAtEpochMs", letter.createdAt().toEpochMilli());
            out.write(MAPPER.writeValueAsString(row) + "\n");
            lines++;
        }
        return lines;
    }
}
