package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.List;

/**
 * 技能排序器 SPI（spec 630 / T910）：候选技能按问法排序的统一面——
 * {@link SemanticSkillRanker}（spec 59 cosine）与 {@link HybridSkillRanker}
 * （spec 605 语义+词法 RRF）同实现，装配面可互换。
 *
 * <p>契约：hint 为 null/空或实现方降级 → 原样返回（保原序稳定）。
 */
public interface SkillRanker {

    /** 排序（实现方保证稳定：并列保原序；降级原样返回）。 */
    List<SkillMetadata> rank(List<SkillMetadata> candidates, String queryHint);
}
