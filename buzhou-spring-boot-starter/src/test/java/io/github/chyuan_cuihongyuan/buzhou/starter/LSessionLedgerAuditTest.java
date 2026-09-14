package io.github.chyuan_cuihongyuan.buzhou.starter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1440 / T2182：L 会话阶段对账（J 会话 j-audit / K 会话 k-audit 先例）——
 * 1400 系工件链四面互证：spec 文件（docs/spec/14NN-*.md）↔ README 纵深行 ↔
 * 票（tickets/T21NN-…shape+verify 成对）↔ impl（impl/10NN-…）。范围自扩展
 * （扫现有 spec 文件驱动——后续轮落地自动纳入对账）；R50 收口轮全量核账。
 */
class LSessionLedgerAuditTest {

    private static final Path REPO = Path.of("..");
    private static final Path SPEC_DIR = REPO.resolve("docs/spec");
    private static final Path TICKET_DIR = REPO.resolve(".wayfinder/tickets");
    private static final Path IMPL_DIR = REPO.resolve(".wayfinder/impl");
    private static final Path README = REPO.resolve("README.md");

    private static final Pattern L_SPEC_PREFIX = Pattern.compile("^14[0-9]{2}-");

    /** 票号公式：R 轮（spec 1400+R-1）→ shape 票 = 2100+2(R-1)+1，verify = shape+1。 */
    private static int shapeTicketFor(int spec) {
        return 2100 + (spec - 1400) * 2 + 1;
    }

    private List<Integer> lSpecNumbers() throws IOException {
        try (Stream<Path> files = Files.list(SPEC_DIR)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> L_SPEC_PREFIX.matcher(n).find())
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
    void everyLSpecHasShapeAndVerifyTicketPair() throws IOException {
        for (int spec : lSpecNumbers()) {
            int shape = shapeTicketFor(spec);
            assertThat(dirHasFileStartingWith(TICKET_DIR, "T" + shape))
                    .as("spec %d 缺 shape 票 T%d", spec, shape).isTrue();
            assertThat(dirHasFileStartingWith(TICKET_DIR, "T" + (shape + 1)))
                    .as("spec %d 缺 verify 票 T%d", spec, shape + 1).isTrue();
        }
    }

    @Test
    void implSlicesExistForAllLSpecs() throws IOException {
        // 容差窗：spec 1439 缺位（1439 号曾被前置占用后让位）使后续 impl 相对公式
        // 平移 -1——按 ±1 窗校验而非精确等式
        for (int spec : lSpecNumbers()) {
            int base = 1052 + (spec - 1400) + 1;
            boolean found = dirHasFileStartingWith(IMPL_DIR, String.valueOf(base))
                    || dirHasFileStartingWith(IMPL_DIR, String.valueOf(base - 1))
                    || dirHasFileStartingWith(IMPL_DIR, String.valueOf(base + 1));
            assertThat(found)
                    .as("spec %d 附近缺 impl 切片 %d±1", spec, base).isTrue();
        }
    }

    @Test
    void readmeCarriesAllLSpecNumbers() throws IOException {
        String readme = Files.readString(README);
        for (int spec : lSpecNumbers()) {
            assertThat(readme.contains(String.valueOf(spec)))
                    .as("README 缺 spec %d 行（覆盖门）", spec).isTrue();
        }
    }

    @Test
    void specNumbersStrictlyIncreasingFrom1400() throws IOException {
        // spec 1439 有意缺位（编号让位入档 spec 1440 头注）——断言严格递增+起点 1400
        List<Integer> specs = lSpecNumbers();
        assertThat(specs).isNotEmpty();
        assertThat(specs.get(0)).isEqualTo(1400);
        for (int i = 1; i < specs.size(); i++) {
            assertThat(specs.get(i)).as("spec 号严格递增（第 %d 个）", i)
                    .isGreaterThan(specs.get(i - 1));
        }
    }
}
