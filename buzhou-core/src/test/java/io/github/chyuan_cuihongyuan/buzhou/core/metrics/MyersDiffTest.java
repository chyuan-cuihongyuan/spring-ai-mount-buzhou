package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6007 / T6216：MyersDiff 合同——最短可执行编辑脚本。
 * 脚本可执行（应用恒得 b + EQUAL 双侧一致）；最优长度 =
 * N+M−2·LCS（DP oracle）；空侧/全等；fail-fast。
 */
class MyersDiffTest {

    private static List<String> apply(List<String> a, List<MyersDiff.Edit> script) {
        List<String> out = new ArrayList<>();
        int ai = 0;
        for (MyersDiff.Edit edit : script) {
            switch (edit.op()) {
                case EQUAL -> {
                    assertThat(edit.text()).isEqualTo(a.get(ai));
                    out.add(a.get(ai++));
                }
                case DELETE -> ai++;
                case INSERT -> out.add(edit.text());
            }
        }
        assertThat(ai).as("脚本消耗完 a 全部元素").isEqualTo(a.size());
        return out;
    }

    private static int lcsLength(List<String> a, List<String> b) {
        int[][] dp = new int[a.size() + 1][b.size() + 1];
        for (int i = 1; i <= a.size(); i++) {
            for (int j = 1; j <= b.size(); j++) {
                dp[i][j] = a.get(i - 1).equals(b.get(j - 1))
                        ? dp[i - 1][j - 1] + 1
                        : Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        return dp[a.size()][b.size()];
    }

    @Test
    void simpleScriptIsExecutable() {
        List<MyersDiff.Edit> script = MyersDiff.diff(
                List.of("a", "b", "c"), List.of("a", "d", "c"));
        assertThat(apply(List.of("a", "b", "c"), script)).containsExactly("a", "d", "c");
        assertThat(script).hasSize(4);
    }

    @Test
    void scriptLengthIsOptimalAgainstDpOracle() {
        Random rng = new Random(6007L);
        for (int trial = 0; trial < 200; trial++) {
            List<String> a = new ArrayList<>();
            List<String> b = new ArrayList<>();
            int n = rng.nextInt(9);
            int m = rng.nextInt(9);
            for (int i = 0; i < n; i++) {
                a.add(String.valueOf((char) ('x' + rng.nextInt(3))));
            }
            for (int i = 0; i < m; i++) {
                b.add(String.valueOf((char) ('x' + rng.nextInt(3))));
            }
            List<MyersDiff.Edit> script = MyersDiff.diff(a, b);
            assertThat(apply(a, script)).as("trial %d 可执行", trial).isEqualTo(b);
            long edits = script.stream().filter(e -> e.op() != MyersDiff.Op.EQUAL).count();
            int optimal = n + m - 2 * lcsLength(a, b);
            assertThat(edits).as("trial %d 最优长度", trial).isEqualTo(optimal);
        }
    }

    @Test
    void emptyAndIdenticalBoundaries() {
        List<MyersDiff.Edit> allInsert = MyersDiff.diff(List.of(), List.of("a", "b"));
        assertThat(allInsert).hasSize(2)
                .allSatisfy(e -> assertThat(e.op()).isEqualTo(MyersDiff.Op.INSERT));
        List<MyersDiff.Edit> allDelete = MyersDiff.diff(List.of("a"), List.of());
        assertThat(allDelete).hasSize(1)
                .allSatisfy(e -> assertThat(e.op()).isEqualTo(MyersDiff.Op.DELETE));
        List<MyersDiff.Edit> identical = MyersDiff.diff(List.of("a", "b"), List.of("a", "b"));
        assertThat(identical).hasSize(2)
                .allSatisfy(e -> assertThat(e.op()).isEqualTo(MyersDiff.Op.EQUAL));
    }

    @Test
    void deterministicSameInputSameScript() {
        List<String> a = List.of("r", "u", "n", "n", "e", "r");
        List<String> b = List.of("r", "u", "nn", "e", "r");
        assertThat(MyersDiff.diff(a, b)).isEqualTo(MyersDiff.diff(a, b));
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> MyersDiff.diff(null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MyersDiff.diff(List.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
