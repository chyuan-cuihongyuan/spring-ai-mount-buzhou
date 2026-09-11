package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 技能目录清单指纹测试（spec 616 / T882–T883 / impl 469，spec 175 工具指纹的 skills 镜像）：
 * 摘要稳定、输入序不敏感、三分类对账、allowedTools 变更计 CHANGED、null/空安全。
 */
class SkillCatalogFingerprintTest {

    private static SkillMetadata skill(String name, String description, String... tools) {
        return new SkillMetadata(name, description, List.of(tools), SkillSource.CLASSPATH);
    }

    /** 摘要稳定 + 输入序不敏感（同集不同序同摘要）。 */
    @Test
    void summaryStableAndOrderInsensitive() {
        SkillCatalogFingerprint a = SkillCatalogFingerprint.of(List.of(
                skill("deploy", "部署"), skill("audit", "审计")));
        SkillCatalogFingerprint b = SkillCatalogFingerprint.of(List.of(
                skill("audit", "审计"), skill("deploy", "部署")));
        assertThat(a.summaryHex()).isEqualTo(b.summaryHex());
        assertThat(a.summaryHex()).hasSize(64); // sha256 hex
        assertThat(a.size()).isEqualTo(2);
    }

    /** 三分类：新增/删除/描述变更各归其类。 */
    @Test
    void diffClassifiesAddedRemovedChanged() {
        SkillCatalogFingerprint before = SkillCatalogFingerprint.of(List.of(
                skill("deploy", "部署"), skill("audit", "审计 v1"), skill("old", "将删")));
        SkillCatalogFingerprint after = SkillCatalogFingerprint.of(List.of(
                skill("deploy", "部署"), skill("audit", "审计 v2"), skill("new", "新增")));

        SkillCatalogFingerprint.Diff diff = after.diff(before);
        assertThat(diff.added()).containsExactly("new");
        assertThat(diff.removed()).containsExactly("old");
        assertThat(diff.changed()).containsExactly("audit");
        assertThat(diff.isEmpty()).isFalse();
    }

    /** allowedTools 变更计 CHANGED（权限面变化必须显形）；同目录 diff 空。 */
    @Test
    void allowedToolsChangeCountsAsChanged() {
        SkillCatalogFingerprint before = SkillCatalogFingerprint.of(List.of(
                skill("query", "查询", "read_db")));
        SkillCatalogFingerprint toolsChanged = SkillCatalogFingerprint.of(List.of(
                skill("query", "查询", "read_db", "write_db")));

        assertThat(toolsChanged.diff(before).changed()).containsExactly("query");
        assertThat(before.diff(before).isEmpty()).isTrue();
    }

    /** null/空目录安全：空摘要可算、对空目录全 REMOVED。 */
    @Test
    void nullAndEmptySafety() {
        SkillCatalogFingerprint empty = SkillCatalogFingerprint.of(null);
        assertThat(empty.size()).isZero();
        assertThat(empty.summaryHex()).hasSize(64);
        SkillCatalogFingerprint one = SkillCatalogFingerprint.of(List.of(skill("a", "d")));
        assertThat(empty.diff(one).removed()).containsExactly("a");
        assertThat(one.diff(null).removed()).containsExactly("a");
    }
}
