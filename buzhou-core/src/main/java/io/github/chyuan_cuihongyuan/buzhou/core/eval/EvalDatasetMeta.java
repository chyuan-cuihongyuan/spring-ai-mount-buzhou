package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.time.Instant;

/**
 * 评估数据集元数据（spec 52 §A / T190）。
 *
 * @param name        数据集名（{@code [a-z0-9-]{1,64}}，create 时校验）
 * @param description 描述（可空）
 * @param itemCount   条目数（addItem 同步维护）
 * @param createdAt   创建时刻
 */
public record EvalDatasetMeta(String name, String description, int itemCount, Instant createdAt) {
}
