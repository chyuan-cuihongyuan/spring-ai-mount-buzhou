package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 705 / T1010–T1011：Redis 键命名空间碰撞审计——三族结构性碰撞被找出、
 * 守卫谓词通过/拒绝清单、定制前缀形状不变。
 */
class RedisKeyLayoutAuditTest {

    @Test
    void structuralCollisionsAreFound() {
        List<RedisKeyLayoutAudit.Finding> findings = RedisKeyLayoutAudit.audit();
        assertThat(findings).extracting(RedisKeyLayoutAudit.Finding::kind)
                .contains("RESERVED_SEGMENT", "SPAN_INDEX_CLASH", "COLON_SUFFIX_TRICK");

        // 跨形状同串的精确断言（实测潜伏碰撞）
        assertThat(findings).extracting(RedisKeyLayoutAudit.Finding::key)
                .contains("buzhou:obs:spev:spans",   // spansOfSession("spev") == eventsOfSpan("spans")
                        "buzhou:obs:event:spans",    // spansOfSession("event") == event("spans")
                        "buzhou:lease:a:seq");       // lease("a:seq") == leaseFencingSeq("a")
        // conflictWith 与 key 同串（完全碰撞的证据形态）
        assertThat(findings).filteredOn(f -> f.kind().equals("COLON_SUFFIX_TRICK"))
                .allSatisfy(f -> assertThat(f.key()).isEqualTo(f.conflictWith()));
    }

    @Test
    void customPrefixKeepsShapeFindings() {
        List<RedisKeyLayoutAudit.Finding> findings = RedisKeyLayoutAudit.audit("x:");
        assertThat(findings).extracting(RedisKeyLayoutAudit.Finding::kind)
                .contains("SPAN_INDEX_CLASH", "COLON_SUFFIX_TRICK");
        assertThat(findings).extracting(RedisKeyLayoutAudit.Finding::key)
                .contains("x:obs:spev:spans", "x:lease:a:seq");
    }

    @Test
    void sessionIdGuardAllowsSafeAndRejectsAdversarial() {
        assertThat(RedisKeyLayoutAudit.isSafeSessionId("sess-123")).isTrue();
        assertThat(RedisKeyLayoutAudit.isSafeSessionId("ok_1.2")).isTrue();

        assertThat(RedisKeyLayoutAudit.isSafeSessionId("a:seq")).isFalse();       // 冒号后缀歧义
        assertThat(RedisKeyLayoutAudit.isSafeSessionId("spev")).isFalse();        // 保留段
        assertThat(RedisKeyLayoutAudit.isSafeSessionId("sessions")).isFalse();    // 保留段
        assertThat(RedisKeyLayoutAudit.isSafeSessionId("")).isFalse();            // 空
        assertThat(RedisKeyLayoutAudit.isSafeSessionId("   ")).isFalse();         // 空白
        assertThat(RedisKeyLayoutAudit.isSafeSessionId(null)).isFalse();          // null
        assertThat(RedisKeyLayoutAudit.isSafeSessionId("a*b")).isFalse();         // glob 元字符
        assertThat(RedisKeyLayoutAudit.reservedSegments()).contains("spev", "event", "sessions");
    }
}
