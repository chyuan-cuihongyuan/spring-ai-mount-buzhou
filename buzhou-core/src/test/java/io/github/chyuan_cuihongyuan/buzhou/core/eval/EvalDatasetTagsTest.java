package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 713 / T1026–T1027：数据集标签——幂等/归一/过滤/旧记录兼容/非法 fail-fast。
 */
class EvalDatasetTagsTest {

    private final BuzhouStores stores = Buzhou.inMemoryStores();

    private EvalDatasetStore storeWithDatasets() {
        EvalDatasetStore store = new EvalDatasetStore(stores.sessionStateStore());
        store.createDataset("faq-smoke", null);
        store.createDataset("regression-v3", null);
        store.createDataset("nightly-judge", null);
        return store;
    }

    @Test
    void taggingIsIdempotentNormalizedAndFilterable() {
        EvalDatasetStore store = storeWithDatasets();
        store.tagDataset("faq-smoke", "Regression");
        store.tagDataset("faq-smoke", "regression"); // 归一后幂等
        store.tagDataset("regression-v3", "regression");
        store.tagDataset("nightly-judge", "judge");

        assertThat(store.dataset("faq-smoke").orElseThrow().tags())
                .containsExactly("regression"); // 归一 + 不重复
        assertThat(store.listDatasetsByTag("REGRESSION"))
                .extracting(EvalDatasetMeta::name)
                .containsExactly("faq-smoke", "regression-v3"); // 按名序
        assertThat(store.listDatasetsByTag("judge"))
                .extracting(EvalDatasetMeta::name)
                .containsExactly("nightly-judge");

        // 去标：不存在的标签幂等原样返回
        store.untagDataset("faq-smoke", "nonexistent-tag");
        assertThat(store.dataset("faq-smoke").orElseThrow().tags()).containsExactly("regression");
        store.untagDataset("faq-smoke", "regression");
        assertThat(store.listDatasetsByTag("regression"))
                .extracting(EvalDatasetMeta::name)
                .containsExactly("regression-v3");
    }

    @Test
    void legacyMetaWithoutTagsFieldDecodesToEmpty() {
        // 旧记录：手工构造无 tags 字段的 meta JSON——解码 tags = 空（向后兼容）
        Map<String, Object> legacy = new java.util.LinkedHashMap<>();
        legacy.put("name", "legacy-ds");
        legacy.put("description", null);
        legacy.put("itemCount", 0);
        legacy.put("nextItemId", 1L);
        legacy.put("createdAt", "2026-01-01T00:00:00Z");
        stores.sessionStateStore().put(EvalDatasetStore.SESSION_ID,
                new io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry(
                        EvalDatasetStore.PREFIX + "legacy-ds", EvalRunner.encode(legacy), "eval", 0, null,
                        java.time.Instant.parse("2026-01-01T00:00:00Z")));
        EvalDatasetStore store = new EvalDatasetStore(stores.sessionStateStore());
        EvalDatasetMeta meta = store.dataset("legacy-ds").orElseThrow();
        assertThat(meta.tags()).isEmpty();
        // tags 不可变——外部改不动（record 规范构造 List.copyOf）
        assertThatThrownBy(() -> meta.tags().add("hack"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(store.dataset("legacy-ds").orElseThrow().tags()).isEmpty();
    }

    @Test
    void invalidTagsFailFast() {
        EvalDatasetStore store = storeWithDatasets();
        assertThatThrownBy(() -> store.tagDataset("faq-smoke", null))
                .isInstanceOf(BuzhouException.class);
        assertThatThrownBy(() -> store.tagDataset("faq-smoke", "   "))
                .isInstanceOf(BuzhouException.class);
        assertThatThrownBy(() -> store.tagDataset("faq-smoke", "a".repeat(33)))
                .isInstanceOf(BuzhouException.class);
        assertThatThrownBy(() -> store.tagDataset("faq-smoke", "bad tag!"))
                .isInstanceOf(BuzhouException.class);
        assertThatThrownBy(() -> store.tagDataset("not-built", "ok-tag"))
                .isInstanceOf(BuzhouException.class); // 数据集未建
    }
}
