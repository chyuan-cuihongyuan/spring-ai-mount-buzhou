package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.transaction.MvccVisibility.RowVersion;
import io.github.chyuan_cuihongyuan.buzhou.core.transaction.MvccVisibility.Snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4040 / T6082：MVCC 可见性合同——创建/删除各象限、
 * repeatable read、收尾 fail-fast。
 */
class MvccVisibilityTest {

    @Test
    void committedBeforeSnapshotShouldBeVisible() {
        MvccVisibility mvcc = new MvccVisibility();
        long writer = mvcc.begin();
        mvcc.commit(writer);
        Snapshot snapshot = mvcc.snapshot();
        assertThat(MvccVisibility.visible(snapshot, RowVersion.created(writer))).isTrue();
    }

    @Test
    void createdAfterSnapshotShouldBeInvisible() {
        MvccVisibility mvcc = new MvccVisibility();
        Snapshot snapshot = mvcc.snapshot();   // 先拍快照
        long lateWriter = mvcc.begin();        // 快照后才开始的事务
        RowVersion row = RowVersion.created(lateWriter);
        assertThat(MvccVisibility.visible(snapshot, row)).isFalse();
        mvcc.commit(lateWriter);
        assertThat(MvccVisibility.visible(snapshot, row)).isFalse();   // 提交了也照旧
        assertThat(MvccVisibility.visible(mvcc.snapshot(), row)).isTrue();   // 新快照可见
    }

    @Test
    void inFlightCreatorShouldSplitOldAndNewSnapshots() {
        MvccVisibility mvcc = new MvccVisibility();
        long writer = mvcc.begin();
        Snapshot beforeCommit = mvcc.snapshot();
        mvcc.commit(writer);
        RowVersion row = RowVersion.created(writer);
        assertThat(MvccVisibility.visible(beforeCommit, row)).isFalse();   // 拍照时未提交
        assertThat(MvccVisibility.visible(mvcc.snapshot(), row)).isTrue();
    }

    @Test
    void abortedCreatorShouldNeverBeVisible() {
        MvccVisibility mvcc = new MvccVisibility();
        long writer = mvcc.begin();
        Snapshot beforeAbort = mvcc.snapshot();
        mvcc.abort(writer);
        RowVersion row = RowVersion.created(writer);
        assertThat(MvccVisibility.visible(beforeAbort, row)).isFalse();
        assertThat(MvccVisibility.visible(mvcc.snapshot(), row)).isFalse();   // 回滚即不存在
    }

    @Test
    void deleterQuadrantsShouldDecideVisibility() {
        MvccVisibility mvcc = new MvccVisibility();
        long creator = mvcc.begin();
        mvcc.commit(creator);
        long inFlightDeleter = mvcc.begin();       // 2：横跨两快照始终在飞
        Snapshot boundary = mvcc.snapshot();
        long laterDeleter = mvcc.begin();          // 3：边界后才提交
        Snapshot beforeDeletes = mvcc.snapshot();
        mvcc.commit(laterDeleter);
        long abortedDeleter = mvcc.begin();        // 4：提交前即回滚
        Snapshot beforeAbort = mvcc.snapshot();
        mvcc.abort(abortedDeleter);

        // 删除事务晚于快照（xmax 之后才分配）→ 删除对读者不存在
        assertThat(MvccVisibility.visible(boundary,
                new RowVersion(creator, laterDeleter))).isTrue();
        // 在飞删除 → 删除对读者不存在
        assertThat(MvccVisibility.visible(boundary,
                new RowVersion(creator, inFlightDeleter))).isTrue();
        assertThat(MvccVisibility.visible(beforeDeletes,
                new RowVersion(creator, inFlightDeleter))).isTrue();
        // 快照前在飞、快照后才提交的删除 → 旧照仍见（一致性读不追认）
        assertThat(MvccVisibility.visible(beforeDeletes,
                new RowVersion(creator, laterDeleter))).isTrue();
        // 先提交删除 → 行消失
        assertThat(MvccVisibility.visible(beforeAbort,
                new RowVersion(creator, laterDeleter))).isFalse();
        // 回滚删除 → 删除从未发生
        assertThat(MvccVisibility.visible(beforeAbort,
                new RowVersion(creator, abortedDeleter))).isTrue();
    }

    @Test
    void sameSnapshotShouldRepeatReadConsistently() {
        MvccVisibility mvcc = new MvccVisibility();
        long writer = mvcc.begin();
        mvcc.commit(writer);
        Snapshot stable = mvcc.snapshot();
        RowVersion row = RowVersion.created(writer);
        boolean first = MvccVisibility.visible(stable, row);
        long noise = mvcc.begin();   // 并发写持续发生
        mvcc.commit(noise);
        boolean second = MvccVisibility.visible(stable, row);
        assertThat(first).isTrue();
        assertThat(second).isEqualTo(first);   // repeatable read 免费成立
    }

    @Test
    void unknownOrDuplicateFinishShouldFailFast() {
        MvccVisibility mvcc = new MvccVisibility();
        assertThatThrownBy(() -> mvcc.commit(99)).isInstanceOf(IllegalArgumentException.class);
        long tx = mvcc.begin();
        mvcc.commit(tx);
        assertThatThrownBy(() -> mvcc.commit(tx)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mvcc.abort(tx)).isInstanceOf(IllegalArgumentException.class);
    }
}
