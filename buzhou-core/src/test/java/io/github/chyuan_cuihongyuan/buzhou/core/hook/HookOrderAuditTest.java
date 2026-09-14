package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1422 / T2146：Hook 顺序碰撞审计——同序碰撞组显形（order 升序、
 * 组内名字典序）、唯一 order 不占报告、空清单哨兵、计数一致。
 */
class HookOrderAuditTest {

    private static BuzhouHook hook(String name, int order) {
        return new BuzhouHook() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public int order() {
                return order;
            }
        };
    }

    @Test
    void emptyListYieldsEmptyReport() {
        var report = HookOrderAudit.analyze(List.of());
        assertThat(report.hookCount()).isZero();
        assertThat(report.collisions()).isEmpty();
        assertThat(report.collisionCount()).isZero();
    }

    @Test
    void uniqueOrdersProduceNoCollisions() {
        var report = HookOrderAudit.analyze(List.of(
                hook("alpha", 10), hook("beta", 20), hook("gamma", 30)));
        assertThat(report.hookCount()).isEqualTo(3);
        assertThat(report.collisionCount()).isZero();
    }

    @Test
    void sameOrderHooksAreGroupedWithSortedNames() {
        var report = HookOrderAudit.analyze(List.of(
                hook("zeta", 20), hook("alpha", 10), hook("mid", 20), hook("solo", 30)));
        assertThat(report.collisionCount()).isEqualTo(1);
        var group = report.collisions().get(0);
        assertThat(group.order()).isEqualTo(20);
        // 组内名字典序（现 ChainComposition 兜底序——脆性所在）
        assertThat(group.hookNames()).containsExactly("mid", "zeta");
        assertThat(report.hookCount()).isEqualTo(4);
    }

    @Test
    void multipleCollisionGroupsSortedByOrder() {
        var report = HookOrderAudit.analyze(List.of(
                hook("b2", 50), hook("a2", 50), hook("x1", 5), hook("y1", 5)));
        assertThat(report.collisionCount()).isEqualTo(2);
        assertThat(report.collisions().get(0).order()).isEqualTo(5);
        assertThat(report.collisions().get(0).hookNames()).containsExactly("x1", "y1");
        assertThat(report.collisions().get(1).order()).isEqualTo(50);
    }

    @Test
    void defaultOrderHooksAllCollide() {
        // BuzhouHook.order() 默认值——全部默认钩子构成一组碰撞（最常见的脆性形态）
        var report = HookOrderAudit.analyze(List.of(hook("d1", 0), hook("d2", 0)));
        assertThat(report.collisionCount()).isEqualTo(1);
        assertThat(report.collisions().get(0).hookNames()).containsExactly("d1", "d2");
    }
}
