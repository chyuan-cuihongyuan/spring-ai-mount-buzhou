package io.github.chyuan_cuihongyuan.buzhou.memory.recall;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 803 / T1108：多路改写融合回归——RRF 跨变体累加/去重不重复出现/
 * 单路故障隔离/生成器故障 fail-open/变体封顶/空查询。
 */
class MultiQueryRetrieverTest {

    private static BuzhouMessage msg(String id, String content) {
        return new BuzhouMessage(id, "s1", 1, 0, Role.USER, content,
                List.of(), null, null, null, java.util.Map.of(), java.time.Instant.now());
    }

    private static RecallSearch.Hit hit(String id, String content, int rank) {
        return new RecallSearch.Hit(msg(id, content), 1, 1.0 / (rank + 1), "text");
    }

    @Test
    void crossVariantHitsOutrankSingleVariant() {
        // 变体 A: [d1, d2]; 变体 B: [d2, d3] —— d2 双变体命中应登顶
        MultiQueryRetriever retriever = new MultiQueryRetriever(
                q -> List.of("A", "B"),
                (variant, limit) -> variant.equals("A")
                        ? List.of(hit("d1", "a", 0), hit("d2", "b", 1))
                        : List.of(hit("d2", "b", 0), hit("d3", "c", 1)));

        MultiQueryRetriever.Result result = retriever.retrieve("q", 10);
        assertThat(result.variantsExecuted()).isEqualTo(2);
        assertThat(result.uniqueHits()).isEqualTo(3);
        assertThat(result.fused().get(0).message().id()).isEqualTo("d2");
        // RRF 精确值：1/(60+1)+1/(60+2) 与单变体名次比较
        double d2 = 1.0 / 61 + 1.0 / 62;
        assertThat(result.fused().get(0).score()).isEqualTo(d2);
        assertThat(result.fused().get(0).mode()).isEqualTo("multi-rrf");
        // d1 与 d3 同名次（第 2）——同分稳定序按首见
        assertThat(result.fused()).hasSize(3);
    }

    @Test
    void sameMessageAcrossVariantsAppearsOnce() {
        MultiQueryRetriever retriever = new MultiQueryRetriever(
                q -> List.of("x", "y", "z"),
                (variant, limit) -> List.of(hit("same", "doc", 0)));
        MultiQueryRetriever.Result result = retriever.retrieve("q", 10);
        assertThat(result.fused()).hasSize(1);
        assertThat(result.uniqueHits()).isEqualTo(1);
        assertThat(result.fused().get(0).score()).isEqualTo(3.0 / 61); // 三路累加
    }

    @Test
    void singleVariantFailsDoNotSinkOthers() {
        AtomicInteger calls = new AtomicInteger();
        MultiQueryRetriever retriever = new MultiQueryRetriever(
                q -> List.of("boom", "ok"),
                (variant, limit) -> {
                    if (variant.equals("boom")) {
                        throw new IllegalStateException("retriever down");
                    }
                    calls.incrementAndGet();
                    return List.of(hit("d1", "doc", 0));
                });
        MultiQueryRetriever.Result result = retriever.retrieve("q", 10);
        assertThat(result.variantsExecuted()).isEqualTo(2);
        assertThat(calls.get()).isEqualTo(1);
        assertThat(result.fused()).hasSize(1);
    }

    @Test
    void variantGeneratorFailureFallsBackToOriginal() {
        MultiQueryRetriever retriever = new MultiQueryRetriever(
                q -> {
                    throw new RuntimeException("llm down");
                },
                (variant, limit) -> List.of(hit("d1", "doc " + variant, 0)));
        MultiQueryRetriever.Result result = retriever.retrieve("orig", 10);
        assertThat(result.variantsExecuted()).isEqualTo(1);
        assertThat(result.fused()).hasSize(1);
        assertThat(result.fused().get(0).message().content()).isEqualTo("doc orig");
    }

    @Test
    void emptyOrNullVariantsFallBackToOriginal() {
        MultiQueryRetriever empty = new MultiQueryRetriever(q -> List.of(),
                (v, l) -> List.of(hit("d1", "doc", 0)));
        assertThat(empty.retrieve("orig", 10).variantsExecuted()).isEqualTo(1);
        MultiQueryRetriever nullish = new MultiQueryRetriever(q -> null,
                (v, l) -> List.of(hit("d1", "doc", 0)));
        assertThat(nullish.retrieve("orig", 10).variantsExecuted()).isEqualTo(1);
    }

    @Test
    void variantsAreCapped() {
        MultiQueryRetriever retriever = new MultiQueryRetriever(
                q -> java.util.stream.IntStream.rangeClosed(1, 20)
                        .mapToObj(i -> "v" + i).toList(),
                (v, l) -> List.of());
        assertThat(retriever.retrieve("q", 10).variantsExecuted())
                .isEqualTo(MultiQueryRetriever.MAX_VARIANTS);
    }

    @Test
    void blankQueryAndLimitEdges() {
        MultiQueryRetriever retriever = new MultiQueryRetriever(q -> List.of("a"),
                (v, l) -> List.of(hit("d1", "doc", 0)));
        assertThat(retriever.retrieve("", 10).fused()).isEmpty();
        assertThat(retriever.retrieve(null, 10).fused()).isEmpty();
        assertThat(retriever.retrieve("q", 0).fused()).hasSize(1); // limit<=0 → 10 默认
    }

    @Test
    void limitTruncatesFusedOutput() {
        MultiQueryRetriever retriever = new MultiQueryRetriever(
                q -> List.of("a"),
                (v, l) -> List.of(hit("d1", "1", 0), hit("d2", "2", 1), hit("d3", "3", 2)));
        assertThat(retriever.retrieve("q", 2).fused()).hasSize(2);
    }

    @Test
    void failFastOnNullFunctions() {
        assertThatThrownBy(() -> new MultiQueryRetriever(null, (v, l) -> List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MultiQueryRetriever(q -> List.of(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
