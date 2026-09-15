package io.github.chyuan_cuihongyuan.buzhou.starter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1800 / T2801：O 会话 1800 系对账门（LSession1700LedgerAuditTest 同款
 * 预防式应用）——150 轮工件链四面互证：spec 文件（docs/spec/，1800–1949 数值
 * 号段——跨 18xx/19xx 双前缀故用区间而非前缀正则）↔ README 纵深行 ↔ 票
 * （tickets/T 对：shape+verify 成对）↔ impl（impl/）。范围自扩展（扫现有
 * spec 文件驱动——后续轮落地自动纳入对账）；R6k 对账轮全量核账。
 */
class OSession1800LedgerAuditTest {

    private static final Path REPO = Path.of("..");
    private static final Path SPEC_DIR = REPO.resolve("docs/spec");
    private static final Path TICKET_DIR = REPO.resolve(".wayfinder/tickets");
    private static final Path IMPL_DIR = REPO.resolve(".wayfinder/impl");
    private static final Path README = REPO.resolve("README.md");

    /** O 系号段：150 轮 = spec 1800–1949（1950+ 为并行缓冲，不入对账）。 */
    private static final int SERIES_MIN = 1800;
    private static final int SERIES_MAX = 1949;

    /** 票号公式：spec N → shape 票 = 2801+2(N−1800)，verify = shape+1。 */
    private static int shapeTicketFor(int spec) {
        return 2801 + (spec - 1800) * 2;
    }

    /** impl 公式：spec N → impl 1401+(N−1800)（本轮系列零缺位承诺）。 */
    private static int implFor(int spec) {
        return 1401 + (spec - 1800);
    }

    private List<Integer> seriesSpecNumbers() throws IOException {
        try (Stream<Path> files = Files.list(SPEC_DIR)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> {
                        int dash = n.indexOf('-');
                        if (dash != 4) {
                            return false;
                        }
                        try {
                            int num = Integer.parseInt(n.substring(0, 4));
                            return num >= SERIES_MIN && num <= SERIES_MAX;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    })
                    .map(n -> Integer.parseInt(n.substring(0, 4)))
                    .distinct()
                    .sorted()
                    .toList();
        }
    }

    private boolean dirHasFileStartingWith(Path dir, String prefix) {
        try (Stream<Path> files = Files.list(dir)) {
            return files.anyMatch(p -> p.getFileName().toString().startsWith(prefix));
        } catch (IOException e) {
            return false;
        }
    }

    @Test
    void everySeriesSpecHasShapeAndVerifyTicketPair() throws IOException {
        for (int spec : seriesSpecNumbers()) {
            int shape = shapeTicketFor(spec);
            assertThat(dirHasFileStartingWith(TICKET_DIR, "T" + shape))
                    .as("spec %d 缺 shape 票 T%d", spec, shape).isTrue();
            assertThat(dirHasFileStartingWith(TICKET_DIR, "T" + (shape + 1)))
                    .as("spec %d 缺 verify 票 T%d", spec, shape + 1).isTrue();
        }
    }

    @Test
    void implSlicesExistForAllSeriesSpecs() throws IOException {
        for (int spec : seriesSpecNumbers()) {
            assertThat(dirHasFileStartingWith(IMPL_DIR, String.valueOf(implFor(spec))))
                    .as("spec %d 缺 impl 切片 %d", spec, implFor(spec)).isTrue();
        }
    }

    @Test
    void readmeCarriesAllSeriesSpecNumbers() throws IOException {
        String readme = Files.readString(README);
        for (int spec : seriesSpecNumbers()) {
            assertThat(readme.contains(String.valueOf(spec)))
                    .as("README 缺 spec %d 行（覆盖门）", spec).isTrue();
        }
    }

    @Test
    void specNumbersStrictlyIncreasingFrom1800() throws IOException {
        List<Integer> specs = seriesSpecNumbers();
        assertThat(specs).isNotEmpty();
        assertThat(specs.get(0)).isEqualTo(SERIES_MIN);
        for (int i = 1; i < specs.size(); i++) {
            assertThat(specs.get(i)).as("spec 号严格递增（第 %d 个）", i)
                    .isGreaterThan(specs.get(i - 1));
        }
    }
}
