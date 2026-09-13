package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * 证据引用失效率读数（spec 843 / T1187，spill 证据引用治理——S3 presigned
 * URL 时限校验思想）：对「被引用的 spill URI 集合」与「存在性判定」做失效率
 * 对账——引用目标已被清扫/删除的证据是断链（模型回读 404）——失效率+失效
 * 样本列表结构化。
 *
 * <p>纯函数：referenced 由调用方自 EvidenceRefLedger/导出采集；existence
 * 判定谓词注入（文件存在/句柄可解析——语义归调用方）；失效样本典序封顶
 * {@value #SAMPLE_LIMIT}。null/空白 URI 忽略。
 */
public final class EvidenceRefValidity {

    /** 失效样本封顶。 */
    public static final int SAMPLE_LIMIT = 16;

    /** 不可变报告。 */
    public record Report(int totalRefs, int validRefs, int invalidRefs,
                         double invalidRatio, List<String> invalidSample) {
    }

    private EvidenceRefValidity() {
    }

    /** 失效率对账（existence=true=有效）。 */
    public static Report audit(Collection<String> referencedUris,
                               Predicate<String> existence) {
        Objects.requireNonNull(existence, "existence");
        int total = 0;
        int valid = 0;
        TreeSet<String> invalid = new TreeSet<>();
        if (referencedUris != null) {
            for (String uri : referencedUris) {
                if (uri == null || uri.isBlank()) {
                    continue;
                }
                total++;
                if (existence.test(uri)) {
                    valid++;
                } else {
                    invalid.add(uri);
                }
            }
        }
        double ratio = total == 0 ? 0 : (double) invalid.size() / total;
        List<String> sample = new ArrayList<>(invalid);
        List<String> limited = sample.size() > SAMPLE_LIMIT
                ? List.copyOf(sample.subList(0, SAMPLE_LIMIT)) : List.copyOf(sample);
        return new Report(total, valid, invalid.size(), ratio, limited);
    }
}
