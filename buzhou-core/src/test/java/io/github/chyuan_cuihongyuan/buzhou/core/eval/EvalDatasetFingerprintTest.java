package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 82 §B / T320：数据集指纹红队——内容寻址（同内容同名/异名等值，改 expected
 * 即变）；run 记录携带（落盘-回读等值）；diff 据此显形就地改项型漂移（同 id 集
 * 漂移也可见）；旧记录（无指纹）不误报。借鉴：LangSmith dataset versioning。
 */
class EvalDatasetFingerprintTest {

    @Test
    void fingerprintIsContentAddressed() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("a", null);
        ds.addItem("a", "q1", "e1", null, null);
        ds.createDataset("b", null);
        ds.addItem("b", "q1", "e1", null, null);

        String fa = ds.fingerprint("a").orElseThrow();
        assertThat(ds.fingerprint("b")).contains(fa); // 同内容异名 = 同版本
        assertThat(ds.fingerprint("a")).contains(fa); // 重读稳定

        ds.addItem("a", "q2", "e2", null, null);
        assertThat(ds.fingerprint("a").map(f -> !f.equals(fa))).contains(true); // 内容变即变
        assertThat(ds.fingerprint("no-such")).isEmpty(); // 不存在 = empty
    }

    @Test
    void runRecordCarriesFingerprintRoundTrip() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("fp", null);
        ds.addItem("fp", "q", "ok", null, null);
        EvalRunner runner = new EvalRunner(
                Buzhou.runtime(prompt -> new org.springframework.ai.chat.model.ChatResponse(
                                java.util.List.of(new org.springframework.ai.chat.model.Generation(
                                        new org.springframework.ai.chat.messages.AssistantMessage("ok")))),
                        stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults()),
                ds, stores.sessionStateStore());

        EvalRunResult run = runner.run("fp", BuiltInEvaluators.EXACT);

        assertThat(run.datasetFingerprint())
                .isEqualTo(ds.fingerprint("fp").orElseThrow());
        // 落盘-回读等值
        assertThat(new EvalQueryService(stores.sessionStateStore())
                .run(run.runId()).orElseThrow().datasetFingerprint())
                .isEqualTo(run.datasetFingerprint());
    }

    @Test
    void diffFlagsInPlaceEditDriftButNotUnknownLegacy() {
        // 同 id 集、同状态，但指纹不同 → drift=true（就地改 expected 型漂移显形）
        Map<String, String> statuses = new LinkedHashMap<>();
        statuses.put("000001", "pass");
        EvalRunResult base = new EvalRunResult("b", "ds",
                java.time.Instant.now(), java.time.Instant.now(), 1, 1, 0, 0,
                java.util.List.of(new EvalRunItemResult("000001", "pass", null, null, 0L)),
                "fp-aaa");
        EvalRunResult edited = new EvalRunResult("h", "ds",
                java.time.Instant.now(), java.time.Instant.now(), 1, 1, 0, 0,
                java.util.List.of(new EvalRunItemResult("000001", "pass", null, null, 0L)),
                "fp-bbb");
        EvalRunResult legacy = EvalRunDiff.runOf("l", "ds", statuses); // 9 参兼容构造 = 无指纹

        assertThat(EvalRunDiff.diff(base, edited).datasetDrift()).isTrue();
        assertThat(EvalRunDiff.diff(base, base).datasetDrift()).isFalse();
        // 单侧未知（旧记录）≠ 漂移（诚实：不猜）
        assertThat(EvalRunDiff.diff(base, legacy).datasetDrift()).isFalse();
        assertThat(EvalRunDiff.diff(legacy, legacy).datasetDrift()).isFalse();
    }
}
