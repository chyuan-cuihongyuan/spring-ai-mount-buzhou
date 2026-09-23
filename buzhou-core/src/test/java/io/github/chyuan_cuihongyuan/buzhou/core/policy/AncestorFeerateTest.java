package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.AncestorFeerate.Tx;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4032 / T6066：祖先费率合同——CPFP 抬父、共享祖先去重、
 * 链式累积、拓扑闭包、环/畸形 fail-fast、确定性并列。
 */
class AncestorFeerateTest {

    @Test
    void valuableChildShouldLiftCheapParent() {
        AncestorFeerate graph = new AncestorFeerate();
        graph.add(new Tx("parent", 100, 100, List.of()));          // 自身费率 1.0
        graph.add(new Tx("child", 1000, 100, List.of("parent")));  // 祖先包费率 1100/200=5.5
        graph.add(new Tx("rich", 500, 100, List.of()));            // 单干者 5.0
        assertThat(graph.ancestorFeerateOf("parent")).isEqualTo(1.0);   // 自己的包不含子孙（诚实）
        assertThat(graph.ancestorFeerateOf("child")).isEqualTo(5.5);
        // 子 5.5 全池最高 → 整包 [parent, child] 先于 5.0 的 rich 入场——父被拖进批次
        assertThat(graph.inclusionOrder()).containsExactly("parent", "child", "rich");
    }

    @Test
    void cheapParentShouldStayLastWithoutChild() {
        AncestorFeerate graph = new AncestorFeerate();
        graph.add(new Tx("cheap", 100, 100, List.of()));
        graph.add(new Tx("rich", 500, 100, List.of()));
        assertThat(graph.inclusionOrder()).containsExactly("rich", "cheap");   // 单笔口径 cheap 殿后
    }

    @Test
    void sharedAncestorShouldCountOnce() {
        AncestorFeerate graph = new AncestorFeerate();
        graph.add(new Tx("root", 100, 100, List.of()));
        graph.add(new Tx("c1", 200, 100, List.of("root")));
        graph.add(new Tx("c2", 300, 100, List.of("root")));
        assertThat(graph.ancestorFeerateOf("c1")).isEqualTo(1.5);   // 300/200
        assertThat(graph.ancestorFeerateOf("c2")).isEqualTo(2.0);   // 400/200——root 不重复计
        assertThat(graph.packageOf("c2")).containsExactly("root", "c2");
    }

    @Test
    void chainShouldAccumulateUpward() {
        AncestorFeerate graph = new AncestorFeerate();
        graph.add(new Tx("g", 100, 100, List.of()));
        graph.add(new Tx("p", 100, 100, List.of("g")));
        graph.add(new Tx("k", 1200, 100, List.of("p")));
        assertThat(graph.ancestorFeerateOf("g")).isEqualTo(1.0);            // 只含自己（向上闭包）
        assertThat(graph.ancestorFeerateOf("k")).isEqualTo(1400.0 / 300);   // 全链累积
        assertThat(graph.packageOf("k")).containsExactly("g", "p", "k");
        assertThat(graph.inclusionOrder()).containsExactly("g", "p", "k");   // k 拖整链
    }

    @Test
    void tieShouldBreakByIdLexicographic() {
        AncestorFeerate graph = new AncestorFeerate();
        graph.add(new Tx("b", 100, 100, List.of()));
        graph.add(new Tx("a", 100, 100, List.of()));
        assertThat(graph.inclusionOrder()).containsExactly("a", "b");
    }

    @Test
    void cycleAndInvalidArgumentsShouldFailFast() {
        AncestorFeerate graph = new AncestorFeerate();
        graph.add(new Tx("x", 100, 100, List.of()));
        assertThatThrownBy(() -> graph.add(new Tx("y", 100, 100, List.of("ghost"))))   // 未知父
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> graph.add(new Tx("x", 100, 100, List.of())))          // 重复 id
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> graph.add(new Tx("z", 100, 0, List.of())))            // 零 size
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> graph.ancestorFeerateOf("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
