package io.github.chyuan_cuihongyuan.buzhou.spill.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillPairAudit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 728 / T1054 族：spill 配对健康面——禁用 UNKNOWN、启用恒 UP、
 * details 四项统计精确。
 */
class SpillPairHealthTest {

    @Test
    void disabledIsUnknownEnabledIsUpWithPairStats(@TempDir Path root) throws Exception {
        assertThat(new SpillPairHealth(false, root).status())
                .isEqualTo(BuzhouHealth.Status.UNKNOWN);
        assertThat(new SpillPairHealth(false, root).details())
                .containsEntry("disabled", true);

        Path session = root.resolve("agent").resolve("s1");
        Files.createDirectories(session);
        Files.write(session.resolve("t1.spill"), "hello".getBytes());
        Files.write(session.resolve("t1.meta"), "{}".getBytes());
        Files.write(session.resolve("t2.spill"), new byte[2048]); // 孤 data

        SpillPairHealth health = new SpillPairHealth(true, root);
        assertThat(health.mechanism()).isEqualTo("spill-pair");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        Map<String, Object> details = health.details();
        assertThat(details.get("dataFiles")).isEqualTo(2L);
        assertThat(details.get("metaFiles")).isEqualTo(1L);
        assertThat(details.get("dataBytes")).isEqualTo(2048L + 5L);
        assertThat(details.get("dataWithoutMeta")).isEqualTo(1L);
        assertThat(details.get("metaWithoutData")).isEqualTo(0L);
    }
}
