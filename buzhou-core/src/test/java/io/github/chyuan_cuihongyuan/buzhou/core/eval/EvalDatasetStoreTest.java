package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 52 §A / T190 / impl-156：评估数据集 store 端到端（合成会话 + 键前缀 + 治理面）。
 */
class EvalDatasetStoreTest {

    @Test
    void datasetLifecycleRoundTrip() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore store = new EvalDatasetStore(stores.sessionStateStore());

        EvalDatasetMeta meta = store.createDataset("regression-basic", "回归基础集");
        assertThat(meta.itemCount()).isZero();
        assertThat(store.dataset("regression-basic")).contains(meta);
        assertThat(store.listDatasets()).extracting(EvalDatasetMeta::name)
                .containsExactly("regression-basic");

        // 键序即添加序 + 元数据计数同步 + 溯源往返
        String id1 = store.addItem("regression-basic", "2+2=?", "4", "sess-a", 7);
        String id2 = store.addItem("regression-basic", "首都?", "北京", null, null);
        assertThat(id1).isEqualTo("000001");
        assertThat(id2).isEqualTo("000002");
        List<EvalItem> items = store.items("regression-basic");
        assertThat(items).hasSize(2);
        assertThat(items.get(0).sourceSessionId()).isEqualTo("sess-a");
        assertThat(items.get(0).sourceTurnSeq()).isEqualTo(7);
        assertThat(items.get(1).sourceSessionId()).isNull();
        assertThat(store.dataset("regression-basic").get().itemCount()).isEqualTo(2);

        // 合成会话写入口径：__buzhou.eval__ 上 eval.ds. 前缀（scanByPrefix 下推面）
        assertThat(stores.sessionStateStore()
                .scanByPrefix("__buzhou.eval__", "eval.ds.").keySet())
                .contains("eval.ds.regression-basic",
                        "eval.ds.regression-basic.item.000001",
                        "eval.ds.regression-basic.item.000002");

        // 删除：元数据 + 条目全清；再删 false
        assertThat(store.deleteDataset("regression-basic")).isTrue();
        assertThat(store.items("regression-basic")).isEmpty();
        assertThat(store.dataset("regression-basic")).isEmpty();
        assertThat(store.deleteDataset("regression-basic")).isFalse();
    }

    @Test
    void rejectsInvalidOperationsWithEvalErrorCode() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore store = new EvalDatasetStore(stores.sessionStateStore());

        // 非法名（大写/下划线/空/超长）
        for (String bad : new String[]{"Bad", "under_score", "", "x".repeat(65)}) {
            assertThatThrownBy(() -> store.createDataset(bad, null))
                    .isInstanceOf(BuzhouException.class)
                    .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                            .isEqualTo(ErrorCode.EVAL_OPERATION_INVALID));
        }
        // 重名
        store.createDataset("dup", null);
        assertThatThrownBy(() -> store.createDataset("dup", null))
                .isInstanceOf(BuzhouException.class);
        // 未建数据集加条目
        assertThatThrownBy(() -> store.addItem("missing", "in", "out", null, null))
                .isInstanceOf(BuzhouException.class)
                .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                        .isEqualTo(ErrorCode.EVAL_OPERATION_INVALID));
        // input/expected 空
        store.createDataset("ok", null);
        assertThatThrownBy(() -> store.addItem("ok", " ", "out", null, null))
                .isInstanceOf(BuzhouException.class);
        assertThatThrownBy(() -> store.addItem("ok", "in", null, null, null))
                .isInstanceOf(BuzhouException.class);
    }

    /** 数据集名相邻前缀不串集（scan 前缀边界：regression 与 regression-2）。 */
    @Test
    void adjacentNamePrefixesDoNotBleed() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore store = new EvalDatasetStore(stores.sessionStateStore());
        store.createDataset("reg", null);
        store.createDataset("reg-2", null);
        store.addItem("reg", "q1", "a1", null, null);

        assertThat(store.items("reg")).hasSize(1);
        assertThat(store.items("reg-2")).isEmpty();
        assertThat(store.listDatasets()).extracting(EvalDatasetMeta::name)
                .containsExactlyInAnyOrder("reg", "reg-2");
        assertThat(store.dataset("reg").get().itemCount()).isEqualTo(1);
        assertThat(store.dataset("reg-2").get().itemCount()).isZero();

        // 删除边界：删 "reg" 不得误删 "reg-2"（前缀串删防护）
        assertThat(store.deleteDataset("reg")).isTrue();
        assertThat(store.dataset("reg")).isEmpty();
        assertThat(store.dataset("reg-2")).isPresent();
    }
}
