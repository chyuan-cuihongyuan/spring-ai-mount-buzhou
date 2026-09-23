package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4043 / T6088：提交图世代号合同——线性链世代读数、菱形
 * max+1、剪枝快道与有界 BFS、倒挂时间戳对照、畸形 fail-fast。
 */
class CommitGraphTest {

    @Test
    void linearChainShouldHaveIncrementingGenerations() {
        CommitGraph graph = new CommitGraph();
        graph.add("a", List.of());
        graph.add("b", List.of("a"));
        graph.add("c", List.of("b"));
        assertThat(graph.generationOf("a")).isEqualTo(1);
        assertThat(graph.generationOf("b")).isEqualTo(2);
        assertThat(graph.generationOf("c")).isEqualTo(3);
        assertThat(graph.isAncestor("a", "c")).isTrue();
        assertThat(graph.isAncestor("c", "a")).isFalse();   // 剪枝快道（gen 3 ≥ 1）
        assertThat(graph.isAncestor("b", "b")).isFalse();   // 相等非祖先
    }

    @Test
    void mergeCommitShouldTakeMaxParentPlusOne() {
        CommitGraph graph = new CommitGraph();
        graph.add("root", List.of());
        graph.add("left", List.of("root"));
        graph.add("right", List.of("root"));   // 标准菱形双腿
        graph.add("merge", List.of("left", "right"));
        assertThat(graph.generationOf("merge")).isEqualTo(3);
        assertThat(graph.isAncestor("right", "merge")).isTrue();
        assertThat(graph.isAncestor("merge", "root")).isFalse();
    }

    @Test
    void generationPruneShouldBeExactWhereTimestampsLie() {
        // 时钟倒挂：c 的提交时间戳早于其祖先 a（commit-date 启发式会误判）
        CommitGraph graph = new CommitGraph();
        graph.add("a", List.of());
        graph.add("b", List.of("a"));
        graph.add("c", List.of("b"));
        long skewTimestampOfA = 2000L;
        long skewTimestampOfC = 1000L;
        boolean timestampHeuristicSaysAncestor = skewTimestampOfA < skewTimestampOfC;
        boolean generationVerdict = graph.isAncestor("a", "c");
        assertThat(timestampHeuristicSaysAncestor).isFalse();   // 时间戳误判（以为不是祖先）
        assertThat(generationVerdict).isTrue();                 // 世代号精确判定
    }

    @Test
    void boundedWalkShouldPruneBelowCandidateGeneration() {
        CommitGraph graph = new CommitGraph();
        graph.add("r", List.of());
        graph.add("m1", List.of("r"));
        graph.add("m2", List.of("m1"));
        graph.add("head", List.of("m2"));
        graph.add("side", List.of("r"));   // 旁支
        assertThat(graph.isAncestor("side", "head")).isFalse();   // gen(side)=2 < gen(head)=4 → BFS 走完不达
        assertThat(graph.isAncestor("m1", "head")).isTrue();
        assertThat(graph.isAncestor("r", "side")).isTrue();
        assertThat(graph.size()).isEqualTo(5);
    }

    @Test
    void unknownParentDuplicateAndUnknownIdShouldFailFast() {
        CommitGraph graph = new CommitGraph();
        assertThatThrownBy(() -> graph.add("x", List.of("ghost")))
                .isInstanceOf(IllegalArgumentException.class);
        graph.add("x", List.of());
        assertThatThrownBy(() -> graph.add("x", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> graph.generationOf("nope"))
                .isInstanceOf(IllegalArgumentException.class);
        graph.add("y", List.of("x"));
        assertThatThrownBy(() -> graph.isAncestor("nope", "y"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
