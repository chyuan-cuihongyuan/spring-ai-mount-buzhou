package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * gzip 导出压缩档位测试（spec 613 / T876–T877 / impl 466，nginx gzip_comp_level 思想）：
 * 档位越高体积不增、两档解压内容逐字节一致、档位校验、缺省重载兼容。
 */
class GzipCompressionLevelTest {

    private static BuzhouStores storesWithObservability() {
        return Buzhou.inMemoryStores();
    }

    /** 同数据 9 档产物 ≤ 1 档（高压缩不劣化）；两档解压后内容一致。 */
    @Test
    void higherLevelNotLargerAndContentIdentical() throws Exception {
        BuzhouStores stores = storesWithObservability();
        ObservabilityJsonlExporter exporter = new ObservabilityJsonlExporter(stores.observabilityStore());

        ByteArrayOutputStream level1 = new ByteArrayOutputStream();
        exporter.exportAllGzip(level1, 1);
        ByteArrayOutputStream level9 = new ByteArrayOutputStream();
        exporter.exportAllGzip(level9, 9);

        assertThat(level9.size()).isLessThanOrEqualTo(level1.size());
        assertThat(decompress(level1.toByteArray())).isEqualTo(decompress(level9.toByteArray()));
    }

    /** 缺省重载（无档位）保持既有行为；返回非空产物可解压。 */
    @Test
    void defaultOverloadStillWorks() throws Exception {
        BuzhouStores stores = storesWithObservability();
        ObservabilityJsonlExporter exporter = new ObservabilityJsonlExporter(stores.observabilityStore());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exporter.exportAllGzip(out);

        assertThat(out.size()).isPositive();
        assertThat(decompress(out.toByteArray())).isNotNull();
    }

    /** 档位越界拒绝（-2 / 10；-1 = Deflater.DEFAULT_COMPRESSION 合法缺省）。 */
    @Test
    void levelValidation() {
        BuzhouStores stores = storesWithObservability();
        ObservabilityJsonlExporter exporter = new ObservabilityJsonlExporter(stores.observabilityStore());

        assertThatThrownBy(() -> exporter.exportAllGzip(new ByteArrayOutputStream(), -2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> exporter.exportAllGzip(new ByteArrayOutputStream(), 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static String decompress(byte[] gzipped) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
