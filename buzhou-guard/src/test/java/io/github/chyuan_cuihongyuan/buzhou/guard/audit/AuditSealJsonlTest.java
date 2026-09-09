package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 421 §Testing / T733–T734：封印导出——一印一行 verified=true；追加
 * 后旧印 false 新印 true；空链零行。
 */
class AuditSealJsonlTest {

    @Test
    void shouldExportSealsWithVerifiedSemantics(@TempDir Path dir) throws Exception {
        AuditChain chain = new AuditChain("agent", "1.0");
        Path jsonl = dir.resolve("seals.jsonl");

        // 空链零行
        assertThat(AuditSealJsonl.appendSeals(chain, jsonl)).isZero();
        assertThat(Files.exists(jsonl)).isFalse();

        for (int i = 0; i < 3; i++) {
            chain.append("s1", "TOOL_CALL", "tool-" + i, "OK");
        }
        chain.sealMerkle(); // 印 1（3 条）
        assertThat(AuditSealJsonl.appendSeals(chain, jsonl)).isEqualTo(1);

        // 追加 2 条后再印——印 2（5 条）
        chain.append("s1", "TOOL_CALL", "tool-3", "OK");
        chain.append("s1", "TOOL_CALL", "tool-4", "OK");
        chain.sealMerkle();
        assertThat(AuditSealJsonl.appendSeals(chain, jsonl)).isEqualTo(2);

        List<String> lines = Files.readAllLines(jsonl);
        assertThat(lines).hasSize(3); // 1 + 2 追加
        // 行 0=第一次导出时的印 1 快照：当时链与印一致 → true（导出时点诚实）
        assertThat(lines.get(0)).contains("\"recordCount\":3").contains("\"verified\":true");
        // 第二次导出的 2 行：旧印（3 条时）当前树根（5 条）≠ 印根 → false 属正常
        assertThat(lines.get(1)).contains("\"recordCount\":3").contains("\"verified\":false");
        // 最新印：verified=true
        assertThat(lines.get(2)).contains("\"recordCount\":5").contains("\"verified\":true");
    }
}
