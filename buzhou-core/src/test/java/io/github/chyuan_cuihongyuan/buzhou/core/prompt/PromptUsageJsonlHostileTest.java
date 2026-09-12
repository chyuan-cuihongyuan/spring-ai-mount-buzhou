package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 644 / T938：PromptUsageJsonl 行序列化 Jackson 化的注入对抗——name
 * 含引号/反斜杠/换行/制表符时行仍合法 JSON 且回读原值（旧手工拼接零转义
 * = 畸形行，OLAP read_json_auto 装载失败）。
 */
class PromptUsageJsonlHostileTest {

    private static final String HOSTILE = "a\"b\\c\nd\re\tf";

    @TempDir
    Path dir;

    @Test
    void hostileNameRoundTripsThroughJsonl() throws Exception {
        PromptUsageStats stats = new PromptUsageStats();
        stats.record(HOSTILE, 1);
        Path f = dir.resolve("usage.jsonl");
        assertThat(PromptUsageJsonl.appendSnapshot(f, stats)).isEqualTo(1);
        String line = Files.readAllLines(f).get(0);
        var node = new ObjectMapper().readTree(line);
        assertThat(node.get("name").asText()).isEqualTo(HOSTILE);
        assertThat(node.get("version").asInt()).isEqualTo(1);
        assertThat(node.get("count").asLong()).isEqualTo(1L);
    }
}
