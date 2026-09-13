package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-697 / spec 949：快照数据集隔离性——源增项靶不变、删源靶活且指纹不变、
 * 靶新增 id 从 max+1 续起不碰撞。
 */
class SnapshotIsolationDeepTest {

    private static EvalDatasetStore store(BuzhouStores stores) {
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("src", null);
        for (int i = 0; i < 3; i++) {
            ds.addItem("src", "问题" + i, "ok", null, null);
        }
        return ds;
    }

    @Test
    void sourceMutationDoesNotAffectTarget() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = store(stores);
        ds.snapshotDataset("src", "snap");

        String fpBefore = ds.fingerprint("snap").orElseThrow();
        ds.addItem("src", "新问题", "ok", null, null); // 源加项

        assertThat(ds.items("snap")).hasSize(3); // 靶不变
        assertThat(ds.fingerprint("snap")).contains(fpBefore); // 指纹稳定
        assertThat(ds.items("src")).hasSize(4);
    }

    @Test
    void deletingSourceKeepsTargetAlive() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = store(stores);
        ds.snapshotDataset("src", "snap");
        String fpTarget = ds.fingerprint("snap").orElseThrow();

        assertThat(ds.deleteDataset("src")).isTrue();
        assertThat(ds.items("snap")).hasSize(3); // 靶独立存活
        assertThat(ds.fingerprint("snap")).contains(fpTarget); // 指纹不变
    }

    @Test
    void targetNewItemIdsContinueFromMax() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = store(stores); // 源已有 id 1..3
        ds.snapshotDataset("src", "snap");

        ds.addItem("snap", "靶上新增", "ok", null, null);
        var ids = ds.items("snap").stream().map(EvalItem::id).toList();
        // 新 id 从 max+1=4 续起（nextItemId 继承语义——不与拷贝项碰撞）
        assertThat(ids).contains("000004");
        assertThat(ids.stream().distinct().count()).isEqualTo(ids.size()); // 无碰撞
    }
}
