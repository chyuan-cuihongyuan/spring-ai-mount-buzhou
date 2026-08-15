package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.session.MediaRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MediaIntake 字节摄取测试（spec 35 §C / T120 / impl-95）：二进制双向无损往返、
 * 文本便捷通道、URI 形态可入 chat、级联删除随会话、空字节拒绝。
 */
class MediaIntakeTest {

    @TempDir
    Path root;

    /** 任意二进制（含 0-255 全字节谱）双向无损。 */
    @Test
    void binaryRoundTripIsLossless() {
        MediaIntake intake = new MediaIntake(new DiskSpillStore(root));
        byte[] bytes = new byte[10_000];
        new SecureRandom().nextBytes(bytes);

        MediaRef ref = intake.intake(bytes, "image/png", "agent-a", "sess-1");

        assertThat(ref.mimeType()).isEqualTo("image/png");
        assertThat(ref.uri().toString()).startsWith("spill://agent-a/sess-1/media-");
        assertThat(intake.readBack(ref)).isEqualTo(bytes);
    }

    /** 文本便捷通道（UTF-8）。 */
    @Test
    void textRoundTripUtf8() {
        MediaIntake intake = new MediaIntake(new DiskSpillStore(root));

        MediaRef ref = intake.intakeText("你好，多媒体 🌏", "text/markdown", "agent-a", "sess-2");

        assertThat(intake.readBackText(ref)).isEqualTo("你好，多媒体 🌏");
    }

    /** 证据随会话级联删除；悬垂回读清晰报错。 */
    @Test
    void cascadesWithSessionAndDanglingReadFailsLoudly() {
        DiskSpillStore store = new DiskSpillStore(root);
        MediaIntake intake = new MediaIntake(store);
        MediaRef ref = intake.intake(new byte[]{1, 2, 3}, "image/png", "agent-a", "sess-3");

        assertThat(store.deleteBySession("agent-a", "sess-3")).isEqualTo(1);

        assertThatThrownBy(() -> intake.readBack(ref))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("媒体证据已被清理");
    }

    /** 空字节拒绝。 */
    @Test
    void emptyBytesRejected() {
        MediaIntake intake = new MediaIntake(new DiskSpillStore(root));
        assertThatThrownBy(() -> intake.intake(new byte[0], "image/png", "a", "s"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
