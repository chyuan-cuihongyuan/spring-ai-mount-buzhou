package io.github.chyuan_cuihongyuan.buzhou.starter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 5000 / T6101：S 会话 5000 系对账门（RSession4000LedgerAuditTest
 * 同款预防式公式族第六应用）——50 轮工件链四面互证：spec 文件
 * （docs/spec/，5000–5049 数值号段）↔ README 纵深行 ↔ 票
 * （tickets/T 对：shape+verify 成对）↔ impl（impl/）。范围自扩展
 * （扫现有 spec 文件驱动——后续轮落地自动纳入对账）；S6k 对账轮
 * 全量核账。
 */
class SSession5000LedgerAuditTest {

    private static final Path REPO = Path.of("..");
    private static final Path SPEC_DIR = REPO.resolve("docs/spec");
    private static final Path TICKET_DIR = REPO.resolve(".wayfinder/tickets");
    private static final Path IMPL_DIR = REPO.resolve(".wayfinder/impl");
    private static final Path README = REPO.resolve("README.md");

    /** S 系号段：50 轮 = spec 5000–5049（2151+ 留给后续会话，不入对账）。 */
    private static final int SERIES_MIN = 5000;
    private static final int SERIES_MAX = 5049;

    /** 票号公式：spec N → shape 票 = 6101+2(N−5000)，verify = shape+1。 */
    private static int shapeTicketFor(int spec) {
        return 6101 + (spec - 5000) * 2;
    }

    /** impl 公式：spec N → impl 2151+(N−5000)（本轮系列零缺位承诺）。 */
    private static int implFor(int spec) {
        return 2151 + (spec - 5000);
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
    void specNumbersStrictlyIncreasingFrom5000() throws IOException {
        List<Integer> specs = seriesSpecNumbers();
        assertThat(specs).isNotEmpty();
        assertThat(specs.get(0)).isEqualTo(SERIES_MIN);
        for (int i = 1; i < specs.size(); i++) {
            assertThat(specs.get(i)).as("spec 号严格递增（第 %d 个）", i)
                    .isGreaterThan(specs.get(i - 1));
        }
    }
}
