package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6006 / T6214：AhoCorasick 合同——Trie+失配链单次扫描
 * 多模式全命中。经典例钉住；重叠命中；暴力圣像；中文；
 * fail-fast。
 */
class AhoCorasickTest {

    private static List<int[]> bruteForce(List<String> patterns, String text) {
        List<int[]> out = new ArrayList<>();
        for (int pi = 0; pi < patterns.size(); pi++) {
            String p = patterns.get(pi);
            for (int s = 0; s + p.length() <= text.length(); s++) {
                if (text.startsWith(p, s)) {
                    out.add(new int[]{s, pi});
                }
            }
        }
        out.sort((a, b) -> {
            int byStart = Integer.compare(a[0], b[0]);
            if (byStart != 0) {
                return byStart;
            }
            return patterns.get(a[1]).compareTo(patterns.get(b[1]));
        });
        return out;
    }

    @Test
    void classicUshersExample() {
        AhoCorasick ac = new AhoCorasick(List.of("he", "she", "his", "hers"));
        List<AhoCorasick.Match> matches = ac.scan("ushers");
        assertThat(matches).containsExactly(
                new AhoCorasick.Match("she", 1, 4),
                new AhoCorasick.Match("he", 2, 4),
                new AhoCorasick.Match("hers", 2, 6));
    }

    @Test
    void overlappingMatchesAllEmitted() {
        AhoCorasick ac = new AhoCorasick(List.of("aa"));
        assertThat(ac.scan("aaaa")).containsExactly(
                new AhoCorasick.Match("aa", 0, 2),
                new AhoCorasick.Match("aa", 1, 3),
                new AhoCorasick.Match("aa", 2, 4));
    }

    @Test
    void randomPatternsMatchBruteForceOracle() {
        Random rng = new Random(6006L);
        List<String> patterns = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            StringBuilder sb = new StringBuilder();
            int len = 1 + rng.nextInt(4);
            for (int j = 0; j < len; j++) {
                sb.append((char) ('a' + rng.nextInt(2)));
            }
            patterns.add(sb.toString());
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            text.append((char) ('a' + rng.nextInt(2)));
        }
        AhoCorasick ac = new AhoCorasick(patterns);
        List<int[]> expected = bruteForce(patterns, text.toString());
        List<AhoCorasick.Match> actual = ac.scan(text.toString());
        assertThat(actual).hasSameSizeAs(expected);
        for (int i = 0; i < expected.size(); i++) {
            int[] e = expected.get(i);
            assertThat(actual.get(i)).isEqualTo(new AhoCorasick.Match(
                    patterns.get(e[1]), e[0], e[0] + patterns.get(e[1]).length()));
        }
    }

    @Test
    void chinesePatternsWork() {
        AhoCorasick ac = new AhoCorasick(List.of("中", "中文", "文中"));
        List<AhoCorasick.Match> matches = ac.scan("中文中文");
        assertThat(matches).containsExactly(
                new AhoCorasick.Match("中", 0, 1),
                new AhoCorasick.Match("中文", 0, 2),
                new AhoCorasick.Match("文中", 1, 3),
                new AhoCorasick.Match("中", 2, 3),
                new AhoCorasick.Match("中文", 2, 4));
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new AhoCorasick(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AhoCorasick(List.of("ok", "")))
                .isInstanceOf(IllegalArgumentException.class);
        java.util.List<String> withNull = new ArrayList<>(List.of("ok"));
        withNull.add(null);
        assertThatThrownBy(() -> new AhoCorasick(withNull))
                .isInstanceOf(IllegalArgumentException.class);
        AhoCorasick ac = new AhoCorasick(List.of("a"));
        assertThatThrownBy(() -> ac.scan(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(ac.scan("bbb")).isEmpty();
        assertThat(ac.patternCount()).isEqualTo(1);
    }
}
