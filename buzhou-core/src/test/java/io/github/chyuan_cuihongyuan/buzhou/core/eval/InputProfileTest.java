package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * impl-691 / spec 942：数据集输入长度画像——count/total/avg 精确、P95 插值、
 * max 显形超长项、空集约定、未建 fail-fast。
 */
class InputProfileTest {

    private static EvalDatasetStore store(BuzhouStores stores, String dataset, int n,
                                          IntFunction<String> input) {
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset(dataset, null);
        for (int i = 0; i < n; i++) {
            ds.addItem(dataset, input.apply(i), "ok", null, null);
        }
        return ds;
    }

    @Test
    void profileStatisticsExact() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = store(stores, "ds-prof", 4,
                i -> "x".repeat((i + 1) * 10)); // 10/20/30/40

        EvalDatasetStore.InputProfile profile = ds.inputLengthProfile("ds-prof");
        assertThat(profile.count()).isEqualTo(4);
        assertThat(profile.totalChars()).isEqualTo(100);
        assertThat(profile.avgChars()).isCloseTo(25.0, within(1e-9));
        assertThat(profile.maxChars()).isEqualTo(40);
        assertThat(profile.p95Chars()).isEqualTo(39); // R-7 插值：h=2.85 → 30+0.85×10=38.5 → round 39
    }

    @Test
    void emptyDatasetAllZero() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = store(stores, "ds-empty", 0, i -> "x");

        EvalDatasetStore.InputProfile profile = ds.inputLengthProfile("ds-empty");
        assertThat(profile.count()).isZero();
        assertThat(profile.totalChars()).isZero();
        assertThat(profile.maxChars()).isZero();
        assertThat(profile.p95Chars()).isZero();
    }

    @Test
    void unknownDatasetTolerantEmptyProfile() {
        // items() 对未知数据集宽容返回空集——画像随之全 0（空集约定一致性）
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        EvalDatasetStore.InputProfile profile = ds.inputLengthProfile("no-such");
        assertThat(profile.count()).isZero();
        assertThat(profile.p95Chars()).isZero();
    }
}
