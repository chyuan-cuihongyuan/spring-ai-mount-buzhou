package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 100 §B / T372：数据集快照红队——内容与指纹与源一致（原 id 复制）；目标已存在
 * fail-fast（版本不可变）；源缺失 fail-fast；快照可续 addItem（nextId 续排）。
 * LangSmith dataset versioning 借鉴（spec 82 fog 收口）。
 */
class EvalDatasetSnapshotTest {

    @Test
    void snapshotPreservesContentFingerprintAndIds() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("live", null);
        ds.addItem("live", "q1", "e1", null, null);
        ds.addItem("live", "q2", "e2", null, null);

        EvalDatasetMeta snapshot = ds.snapshotDataset("live", "live-v1");

        assertThat(snapshot.name()).isEqualTo("live-v1");
        assertThat(snapshot.itemCount()).isEqualTo(2);
        assertThat(ds.fingerprint("live-v1")).isEqualTo(ds.fingerprint("live")); // 同版本
        assertThat(ds.items("live-v1")).usingRecursiveComparison()
                .ignoringFields("createdAt") // 复制保留原 createdAt，但 record 级比较防御式忽略
                .isEqualTo(ds.items("live"));
        assertThat(ds.items("live-v1")).allSatisfy(i -> assertThat(i.input()).startsWith("q"));
    }

    @Test
    void snapshotTargetImmutableAndSourceRequired() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("src", null);
        ds.addItem("src", "q", "e", null, null);
        ds.snapshotDataset("src", "frozen");

        assertThatThrownBy(() -> ds.snapshotDataset("src", "frozen"))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("快照不可覆盖");
        assertThatThrownBy(() -> ds.snapshotDataset("no-such", "x-v1"))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("源数据集未建");
    }

    @Test
    void snapshotSupportsAppendedItemsWithContinuedIds() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("base", null);
        ds.addItem("base", "q1", "e1", null, null);
        ds.addItem("base", "q2", "e2", null, null);
        ds.snapshotDataset("base", "base-v1");

        String appended = ds.addItem("base-v1", "q3", "e3", null, null);

        assertThat(appended).isEqualTo("000003"); // nextId 从 max+1 续排
        assertThat(ds.items("base-v1")).hasSize(3);
        assertThat(ds.fingerprint("base-v1")).isNotEqualTo(ds.fingerprint("base")); // 演化分叉
    }
}
