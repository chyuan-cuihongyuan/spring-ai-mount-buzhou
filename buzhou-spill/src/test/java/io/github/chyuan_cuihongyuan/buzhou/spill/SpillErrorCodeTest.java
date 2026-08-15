package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 50 §A / T178 / impl-147：spill IO 失败面错误码——BuzhouException + SPILL_IO_FAILED
 * （cause 保留；此前为裸 UncheckedIOException）。
 */
class SpillErrorCodeTest {

    @TempDir
    Path tmp;

    @Test
    void ioFailureCarriesSpillIoFailedCode() throws Exception {
        // 根目录路径被同名文件占据 → createDirectories 失败 → SPILL_IO_FAILED
        Path occupied = tmp.resolve("occupied-root");
        Files.writeString(occupied, "not a directory");
        DiskSpillStore store = new DiskSpillStore(occupied);
        SpillEntry entry = new SpillEntry(
                new SpillUri("sess-err", "agent", "spill-x"), "内容", "text/plain", 3, Instant.now());

        assertThatThrownBy(() -> store.store(entry, 20))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("Spill store failed")
                .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                        .isEqualTo(ErrorCode.SPILL_IO_FAILED))
                .hasCauseInstanceOf(java.io.IOException.class);
    }
}
