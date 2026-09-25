package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6008 / T6218：BkTree 合同——度量三角剪枝的编辑距离
 * 邻域索引。经典例钉住；暴力圣像（集+字典序）；幂等；
 * fail-fast。
 */
class BkTreeTest {

    private static int bruteDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1]
                        : 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[a.length()][b.length()];
    }

    @Test
    void classicSpellCandidates() {
        BkTree tree = new BkTree();
        for (String w : new String[]{"book", "back", "buck", "bock"}) {
            tree.add(w);
        }
        assertThat(tree.query("back", 0)).containsExactly("back");
        assertThat(tree.query("back", 1)).containsExactly("back", "bock", "buck");
        assertThat(tree.query("back", 2)).containsExactly("back", "bock", "book", "buck");
    }

    @Test
    void randomDictionaryMatchesBruteForceOracle() {
        Random rng = new Random(6008L);
        List<String> dict = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            StringBuilder sb = new StringBuilder();
            int len = 3 + rng.nextInt(5);
            for (int j = 0; j < len; j++) {
                sb.append((char) ('a' + rng.nextInt(3)));
            }
            dict.add(sb.toString());
        }
        BkTree tree = new BkTree();
        for (String w : dict) {
            tree.add(w);
        }
        assertThat(tree.size()).as("重复词幂等后词数").isEqualTo(dict.stream().distinct().count());
        for (int q = 0; q < 60; q++) {
            StringBuilder sb = new StringBuilder();
            int len = 3 + rng.nextInt(5);
            for (int j = 0; j < len; j++) {
                sb.append((char) ('a' + rng.nextInt(3)));
            }
            String query = sb.toString();
            int radius = rng.nextInt(3);
            List<String> expected = dict.stream().distinct()
                    .filter(w -> bruteDistance(w, query) <= radius)
                    .sorted()
                    .toList();
            assertThat(tree.query(query, radius))
                    .as("query %s r=%d", query, radius)
                    .containsExactlyElementsOf(expected);
        }
    }

    @Test
    void duplicateAddIsIdempotent() {
        BkTree tree = new BkTree();
        tree.add("alpha");
        tree.add("alpha");
        tree.add("beta");
        assertThat(tree.size()).isEqualTo(2);
        assertThat(tree.query("alpha", 0)).containsExactly("alpha");
    }

    @Test
    void failFastContract() {
        BkTree tree = new BkTree();
        assertThatThrownBy(() -> tree.add(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.query(null, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.query("word", -1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(tree.query("anything", 3)).isEmpty();
    }
}
