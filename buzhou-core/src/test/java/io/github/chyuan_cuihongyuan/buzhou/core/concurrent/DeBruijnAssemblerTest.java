package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DeBruijnAssembler 契约测试（spec 10021 / X10044）：完美重叠重构
 * 圣像 + 读段序无关 + 重复 k-mer 重边 + 非欧拉 fail-fast + 契约面。
 */
class DeBruijnAssemblerTest {

    @Test
    void shouldReconstructGenomeFromPerfectReads() {
        String genome = "ACGTTGCAAT";
        List<String> reads = kMers(genome, 4);
        assertThat(DeBruijnAssembler.assemble(reads, 4)).isEqualTo(genome);
    }

    @Test
    void shouldBeOrderIndependent() {
        String genome = "ACGTTGCAAT";
        List<String> reads = kMers(genome, 4);
        Collections.reverse(reads);
        assertThat(DeBruijnAssembler.assemble(reads, 4)).isEqualTo(genome);
    }

    @Test
    void shouldHandleRepeatedKmersAsMultiedges() {
        String genome = "AAAAACAAA";
        List<String> reads = kMers(genome, 5);
        assertThat(DeBruijnAssembler.assemble(reads, 5)).isEqualTo(genome);
    }

    @Test
    void shouldFailFastOnNonEulerianReadSet() {
        List<String> branchy = List.of("ACGT", "ACGA", "TTGA");
        assertThatThrownBy(() -> DeBruijnAssembler.assemble(branchy, 4))
                .isInstanceOf(IllegalArgumentException.class);
        List<String> disjoint = List.of("ACGT", "TTGA");
        assertThatThrownBy(() -> DeBruijnAssembler.assemble(disjoint, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> DeBruijnAssembler.assemble(null, 4))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DeBruijnAssembler.assemble(List.of("ACGT"), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DeBruijnAssembler.assemble(List.of("ACGX"), 4))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DeBruijnAssembler.assemble(List.of("ACG"), 4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private List<String> kMers(String genome, int k) {
        List<String> reads = new ArrayList<>();
        for (int i = 0; i + k <= genome.length(); i++) {
            reads.add(genome.substring(i, i + k));
        }
        return reads;
    }
}
