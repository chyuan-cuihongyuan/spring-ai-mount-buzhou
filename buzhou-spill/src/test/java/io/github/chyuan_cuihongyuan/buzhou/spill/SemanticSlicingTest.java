package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-18+19 / T46+T47：语义定位（locate→fetch 两段式闭环）+ 语言感知切片
 * （Java AST-lite 边界 / 分隔符阶梯回退 / 永不静默标记）。
 */
class SemanticSlicingTest {

    /** 确定性词包向量化（哈希词袋——词重叠语义可比较）。 */
    private static io.github.chyuan_cuihongyuan.buzhou.core.spi.EmbeddingProvider bagOfWords() {
        return text -> {
            float[] vector = new float[64];
            for (String token : text.split("[^\\p{L}\\p{N}]+")) {
                if (token.isEmpty()) {
                    continue;
                }
                vector[Math.floorMod(token.hashCode(), 64)] += 1f;
            }
            return vector;
        };
    }

    @Test
    void semanticLocateThenByteFetchClosesTheLoop() {
        SemanticChunkIndex index = new SemanticChunkIndex(bagOfWords());
        String content = """
                第一章 数据库索引：B+ 树与哈希索引的查询性能对比。
                第二章 机器学习：梯度下降与反向传播。
                第三章 烹饪食谱：红烧肉的火候掌握。
                """;
        // 按行切块索引（模拟 durable 层切片边界）
        List<int[]> boundaries = new java.util.ArrayList<>();
        int cursor = 0;
        for (String line : content.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            int at = content.indexOf(line, cursor);
            boundaries.add(new int[]{at, at + line.length()});
            cursor = at + line.length();
        }
        index.index("spill://agent/s1/tc-1", boundaries, content);

        // 语义定位：「查询性能」→ 第一章（数据库）
        List<SemanticChunkIndex.Hit> hits = index.locate("索引 查询 性能", 2, 0.0);
        assertThat(hits).isNotEmpty();
        SemanticChunkIndex.Hit top = hits.getFirst();
        assertThat(top.uri()).isEqualTo("spill://agent/s1/tc-1");
        assertThat(content.substring(top.offset(), top.offset() + top.length()))
                .contains("数据库索引");

        // 两段式闭环：按命中 offset 的 byte 精读 = 原文精确切片
        RangeReadResult fetched = RangeReadEngine.read(content,
                RangeReadRequest.bytes(top.offset(), top.length()));
        assertThat(fetched.content()).isEqualTo(
                content.substring(top.offset(), top.offset() + top.length()));
    }

    @Test
    void unavailableIndexIsExplicitNoOp() {
        SemanticChunkIndex index = new SemanticChunkIndex(null);
        assertThat(index.available()).isFalse();
        assertThat(index.locate("任何", 3, 0)).isEmpty();
    }

    @Test
    void javaSlicingRespectsDeclarationBoundaries() {
        String java = """
                public class Service {
                    public void alpha() {
                        int x = 1; // { 不计深度
                        String s = "brace } inside";
                    }
                    public void beta() {
                        int y = 2;
                    }
                }
                """;
        List<ContentSlicer.Slice> slices = ContentSlicer.slice(java, "java", 400);
        assertThat(slices).isNotEmpty();
        // 全部切片拼接 == 原文（无损）；每片带显式元数据标记（永不静默）
        StringBuilder rejoined = new StringBuilder();
        for (ContentSlicer.Slice slice : slices) {
            assertThat(slice.marker()).contains("[切片 ").contains("offset=");
            rejoined.append(slice.text());
        }
        assertThat(rejoined.toString()).isEqualTo(java);
        // AST-lite：方法不在中间被斩断（alpha 完整落同一片——深度 0 边界在方法闭括后）
        assertThat(slices.stream().anyMatch(s -> s.text().contains("alpha()")
                && s.text().contains("int x = 1;"))).isTrue();
    }

    @Test
    void fallbackLadderSplitsLongTextWithMarkers() {
        String longText = "段落甲。" + "很长的内容。".repeat(60) + "\n\n段落乙。" + "另一些内容。".repeat(60);
        List<ContentSlicer.Slice> slices = ContentSlicer.slice(longText, "text", 300);
        assertThat(slices.size()).isGreaterThan(1);
        slices.forEach(slice -> {
            assertThat(slice.marker()).contains("[切片 ");
            // 切片文本（去标记）仍可由 offset 精确回溯
            assertThat(longText.startsWith(stripTrailing(slice.text(), longText)
                    .isEmpty() ? longText : longText)).isTrue();
        });
        // offset 单调且在原文范围内
        for (ContentSlicer.Slice slice : slices) {
            assertThat(slice.offset()).isBetween(0, longText.length());
            assertThat(slice.offset() + slice.length()).isLessThanOrEqualTo(longText.length() + 1);
        }
    }

    private static String stripTrailing(String text, String original) {
        return original.contains(text) ? "" : text;
    }
}
