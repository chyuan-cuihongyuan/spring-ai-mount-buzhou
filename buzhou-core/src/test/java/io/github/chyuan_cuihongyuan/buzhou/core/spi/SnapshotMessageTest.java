package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SnapshotMessage} 直测（spec 1204 / T1813 / K 会话 R5——miss≥1 收紧判据浮出，
 * R1 miss≥5 门槛下不可见）。compact 构造的 null 防御是注入快照还原「模型当时看到什么」
 * 的读面合同：metadata 缺省 = 空 Map 不可 null。
 */
class SnapshotMessageTest {

    @Test
    void nullMetadataDefaultsToEmptyMap() {
        SnapshotMessage message = new SnapshotMessage(
                "ASSISTANT", "（微压缩占位符）", "ev-1", null, null);

        assertThat(message.metadata()).isEmpty();
        assertThat(message.role()).isEqualTo("ASSISTANT");
        assertThat(message.content()).contains("微压缩占位符");
        assertThat(message.evidenceId()).isEqualTo("ev-1");
        assertThat(message.spillUri()).isNull();
    }

    @Test
    void providedMetadataIsDefensivelyCopied() {
        Map<String, Object> original = new java.util.HashMap<>();
        original.put("tool_call_id", "tc-1");

        SnapshotMessage message = new SnapshotMessage(
                "TOOL", "工具输出", null, "spill://s1/3", original);
        original.put("mutated-after", true);

        assertThat(message.metadata()).containsOnlyKeys("tool_call_id");
        assertThat(message.spillUri()).isEqualTo("spill://s1/3");
    }
}
