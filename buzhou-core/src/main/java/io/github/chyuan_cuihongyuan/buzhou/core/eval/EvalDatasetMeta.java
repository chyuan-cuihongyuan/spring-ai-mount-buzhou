package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.time.Instant;
import java.util.List;

/**
 * 评估数据集元数据（spec 52 §A / T190）。
 *
 * @param name        数据集名（{@code [a-z0-9-]{1,64}}，create 时校验）
 * @param description 描述（可空）
 * @param itemCount   条目数（addItem 同步维护）
 * @param createdAt   创建时刻
 * @param tags        标签（spec 713 / T1026：归一化升序不可变；组织维度——
 *                    不参与 fingerprint；4 参兼容构造 = 无标签）
 */
public record EvalDatasetMeta(String name, String description, int itemCount, Instant createdAt,
        List<String> tags) {

    /** 4 参兼容构造（spec 713 之前调用方；tags = 空）。 */
    public EvalDatasetMeta(String name, String description, int itemCount, Instant createdAt) {
        this(name, description, itemCount, createdAt, List.of());
    }

    public EvalDatasetMeta {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
