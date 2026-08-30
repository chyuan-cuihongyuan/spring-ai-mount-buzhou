package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 140 §B / T465：技能使用统计红队——计数累积与稳定排序；零使用清单
 * （目录治理证据面）；封顶折 __overflow__（既有技能继续细分）；窗口清零；
 * 空白名拒绝。借鉴：Backstage catalog score / Caffeine 观测面。
 */
class SkillUsageStatsTest {

    @AfterEach
    void cleanup() {
        SkillUsageStats.install(null);
    }

    @Test
    void loadsAccumulateAndRankStably() {
        SkillUsageStats stats = SkillUsageStats.create();
        stats.recordLoad("git-commit");
        stats.recordLoad("git-commit");
        stats.recordLoad("git-commit");
        stats.recordLoad("doc-arch");
        stats.recordLoad("doc-arch");
        stats.recordLoad("zz-tool");

        List<SkillUsageStats.SkillUsage> top = stats.topUsed(3);
        assertThat(top).extracting(SkillUsageStats.SkillUsage::skill)
                .containsExactly("git-commit", "doc-arch", "zz-tool");
        assertThat(top.get(0).loads()).isEqualTo(3);

        // 同 count：名字典序稳定（doc-arch 与 zz-tool 并列 2 次，doc-arch 在前）
        stats.recordLoad("zz-tool");
        assertThat(stats.topUsed(3)).extracting(SkillUsageStats.SkillUsage::skill)
                .containsExactly("git-commit", "doc-arch", "zz-tool");
    }

    @Test
    void unusedListsCatalogSkillsNeverLoaded() {
        SkillUsageStats stats = SkillUsageStats.create();
        stats.recordLoad("hot-skill");
        List<String> unused = stats.unused(
                List.of("hot-skill", "cold-a", "cold-b"));
        assertThat(unused).containsExactly("cold-a", "cold-b"); // 字典序
        stats.recordLoad("all-used");
        assertThat(stats.unused(List.of("all-used"))).isEmpty(); // 全在用 → 空清单
    }

    @Test
    void overflowFoldsNewSkillsButExistingKeepCounting() {
        SkillUsageStats stats = SkillUsageStats.create();
        for (int i = 0; i < SkillUsageStats.MAX_SKILLS; i++) {
            stats.recordLoad("skill-" + i);
        }
        assertThat(stats.distinct()).isEqualTo(SkillUsageStats.MAX_SKILLS);

        stats.recordLoad("skill-0"); // 既有继续细分（唯一 count=2 者）
        stats.recordLoad("brand-new"); // 新名折 overflow（count=1）
        // 封顶语义 = 新技能名不再扩张；overflow 占位是唯一例外键（+1）
        assertThat(stats.distinct()).isEqualTo(SkillUsageStats.MAX_SKILLS + 1);
        stats.recordLoad("brand-new-2"); // 折入既有 overflow 键，不再增长
        assertThat(stats.distinct()).isEqualTo(SkillUsageStats.MAX_SKILLS + 1);
        // brand-new-2 也折入后 overflow=2 与 skill-0 并列：'_' < 's'，overflow 居首
        assertThat(stats.topUsed(2)).extracting(SkillUsageStats.SkillUsage::skill)
                .containsExactly(SkillUsageStats.OVERFLOW, "skill-0");
    }

    @Test
    void resetClearsWindowAndRankingRestarts() {
        SkillUsageStats stats = SkillUsageStats.create();
        stats.recordLoad("old-window");
        stats.reset();
        assertThat(stats.distinct()).isZero();
        stats.recordLoad("new-window");
        assertThat(stats.topUsed(1)).extracting(SkillUsageStats.SkillUsage::skill)
                .containsExactly("new-window");
    }

    @Test
    void blankNameRejected() {
        SkillUsageStats stats = SkillUsageStats.create();
        assertThatThrownBy(() -> stats.recordLoad(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stats.recordLoad(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
