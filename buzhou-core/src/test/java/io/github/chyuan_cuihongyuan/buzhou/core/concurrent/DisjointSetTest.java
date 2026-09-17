package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3002 / T5006：并查集合同——单例初态、传递闭包、冗余合并
 * 不动账、组件计数守恒、组件大小聚合、路径压缩语义稳定、双组件
 * 互斥、越界诚实拒绝。
 */
class DisjointSetTest {

    @Test
    void initialUniverseShouldBeAllSingletons() {
        DisjointSet ds = new DisjointSet(4);
        assertThat(ds.componentCount()).isEqualTo(4);
        assertThat(ds.capacity()).isEqualTo(4);
        for (int i = 0; i < 4; i++) {
            assertThat(ds.find(i)).isEqualTo(i);
            assertThat(ds.sizeOf(i)).isEqualTo(1);
            assertThat(ds.connected(i, i)).isTrue();
        }
    }

    @Test
    void unionShouldCloseTransitivity() {
        DisjointSet ds = new DisjointSet(5);
        assertThat(ds.union(1, 2)).isTrue();
        assertThat(ds.union(2, 3)).isTrue();
        // A=B、B=C ⇒ A=C（传递闭包由单根结构保证）
        assertThat(ds.connected(1, 3)).isTrue();
        assertThat(ds.find(1)).isEqualTo(ds.find(3));
    }

    @Test
    void redundantUnionShouldReturnFalseAndKeepLedger() {
        DisjointSet ds = new DisjointSet(4);
        assertThat(ds.union(0, 1)).isTrue();
        int countAfterFirst = ds.componentCount();
        assertThat(ds.union(1, 0)).isFalse();
        assertThat(ds.union(0, 0)).isFalse();
        assertThat(ds.componentCount()).isEqualTo(countAfterFirst);
    }

    @Test
    void componentCountShouldDecrementOnlyPerEffectiveUnion() {
        DisjointSet ds = new DisjointSet(5);
        assertThat(ds.componentCount()).isEqualTo(5);
        ds.union(0, 1);
        ds.union(2, 3);
        assertThat(ds.componentCount()).isEqualTo(3);
        ds.union(1, 0);
        assertThat(ds.componentCount()).isEqualTo(3);
        ds.union(3, 4);
        assertThat(ds.componentCount()).isEqualTo(2);
        ds.union(0, 4);
        assertThat(ds.componentCount()).isEqualTo(1);
    }

    @Test
    void sizeOfShouldAggregateChainMembers() {
        DisjointSet ds = new DisjointSet(6);
        ds.union(0, 1);
        ds.union(1, 2);
        ds.union(2, 3);
        assertThat(ds.sizeOf(0)).isEqualTo(4);
        assertThat(ds.sizeOf(3)).isEqualTo(4);
        assertThat(ds.sizeOf(4)).isEqualTo(1);
    }

    @Test
    void repeatedFindShouldStayStableAfterCompression() {
        DisjointSet ds = new DisjointSet(6);
        ds.union(0, 1);
        ds.union(1, 2);
        ds.union(2, 3);
        int first = ds.find(3);
        int again = ds.find(3);
        int distant = ds.find(0);
        assertThat(again).isEqualTo(first);
        assertThat(distant).isEqualTo(first);
    }

    @Test
    void disjointComponentsShouldStayDistinct() {
        DisjointSet ds = new DisjointSet(4);
        ds.union(0, 1);
        ds.union(2, 3);
        assertThat(ds.connected(0, 2)).isFalse();
        assertThat(ds.connected(1, 3)).isFalse();
        assertThat(ds.componentCount()).isEqualTo(2);
    }

    @Test
    void outOfUniverseShouldFailFast() {
        DisjointSet ds = new DisjointSet(3);
        assertThatThrownBy(() -> ds.find(-1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> ds.find(3)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> ds.union(0, 3)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> ds.sizeOf(99)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void emptyUniverseShouldBeHonest() {
        DisjointSet ds = new DisjointSet(0);
        assertThat(ds.componentCount()).isZero();
        assertThat(ds.capacity()).isZero();
        assertThatThrownBy(() -> ds.find(0)).isInstanceOf(IndexOutOfBoundsException.class);
    }
}
