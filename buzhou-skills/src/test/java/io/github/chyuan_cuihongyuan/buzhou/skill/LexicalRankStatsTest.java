package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillScanner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LexicalRankStatsTest {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DefaultSkillRegistry registry() {
        return new DefaultSkillRegistry(
                (Map) new ClasspathSkillScanner().scan(), null, null, false, 64);
    }

    @Test
    void blankHintDoesNotCountRun() {
        LexicalSkillRanker ranker = new LexicalSkillRanker();

        ranker.rank(java.util.List.of(), "  ");

        assertThat(ranker.stats()).isEqualTo(new LexicalSkillRanker.RankStats(0, 0));
    }

    @Test
    void validRunCountedForMultiCandidateRank() {
        LexicalSkillRanker ranker = new LexicalSkillRanker();
        var metas = registry().listAllFor("app", "agent");
        org.junit.jupiter.api.Assumptions.assumeTrue(metas.size() >= 2);

        ranker.rank(metas, "code");

        assertThat(ranker.stats().runs()).isEqualTo(1);
    }

    @Test
    void reorderedNeverExceedsRuns() {
        LexicalSkillRanker ranker = new LexicalSkillRanker();
        var metas = registry().listAllFor("app", "agent");
        org.junit.jupiter.api.Assumptions.assumeTrue(metas.size() >= 2);

        ranker.rank(metas, "code");
        ranker.rank(metas, "code");

        LexicalSkillRanker.RankStats stats = ranker.stats();
        assertThat(stats.runs()).isEqualTo(2);
        assertThat(stats.reordered()).isLessThanOrEqualTo(stats.runs());
    }
}
