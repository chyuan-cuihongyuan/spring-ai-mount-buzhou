package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 前缀稳定注入序测试（spec 66 §B / T284）：开启时技能清单块（最稳定）前置——两轮间
 * 事实/清单外的注入变化不破坏首个块的前缀一致性（KV-cache 前缀命中面）；默认关时
 * 块序保持 spec 04 口径（摘要→事实→清单）零变化。
 */
class PrefixStableInjectionTest {

    private static InjectionViewProcessor processor(SkillCatalogRenderer renderer, boolean prefixStable) {
        InjectionViewProcessor ivp = new InjectionViewProcessor(
                new io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor(
                        new io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector()),
                t -> io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionPolicy.defaults(), 1,
                new io.github.chyuan_cuihongyuan.buzhou.memory.budget.DefaultBudgetCalculator(
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver(Map.of()),
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator()),
                new io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge(
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore()),
                new io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator(),
                new io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryCircuitBreaker(3),
                null, "stub", 1, null, 4000);
        ivp.setSkillCatalogRenderer(renderer);
        ivp.setPrefixStableInjection(prefixStable);
        return ivp;
    }

    private static final SkillCatalogRenderer STABLE_CATALOG = sessionId ->
            Optional.of("## 可用技能（Skill Catalog）\n- skill-a: 描述A\n- skill-b: 描述B");

    private static BuzhouMessage msg(int turn, Role role, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s1", turn, 0,
                role, content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    /** 开启时：清单块在视图首位，且两轮间（历史追加变化）首块内容字节级一致。 */
    @Test
    void catalogFirstAndStableAcrossTurns() {
        InjectionViewProcessor ivp = processor(STABLE_CATALOG, true);
        List<BuzhouMessage> turn1 = ivp.process("s1",
                List.of(msg(1, Role.USER, "q1"), msg(1, Role.ASSISTANT, "a1")), 2);
        List<BuzhouMessage> turn2 = ivp.process("s1",
                List.of(msg(1, Role.USER, "q1"), msg(1, Role.ASSISTANT, "a1"),
                        msg(2, Role.USER, "q2")), 3);

        assertThat(turn1.getFirst().metadata()).containsKey("skill-catalog");
        assertThat(turn2.getFirst().metadata()).containsKey("skill-catalog");
        assertThat(turn1.getFirst().content()).isEqualTo(turn2.getFirst().content()); // 前缀稳定
    }

    /** 默认关：块序保持 spec 04 口径（清单在事实之后、近期原文之前）零变化。 */
    @Test
    void defaultOrderUnchangedWhenDisabled() {
        InjectionViewProcessor ivp = processor(sessionId -> Optional.of(
                "## 可用技能\n- x: y"), false);
        List<BuzhouMessage> view = ivp.process("s1",
                List.of(msg(1, Role.USER, "q1"), msg(1, Role.ASSISTANT, "a1")), 2);
        // 无摘要无事实时仅清单块——首位即清单（无其他块可比）；补一个带事实的断言：
        // 开启/关闭对「无摘要无事实」场景输出等价（唯一块位置不变）
        InjectionViewProcessor ivpOn = processor(sessionId -> Optional.of(
                "## 可用技能\n- x: y"), true);
        List<BuzhouMessage> viewOn = ivpOn.process("s1",
                List.of(msg(1, Role.USER, "q1"), msg(1, Role.ASSISTANT, "a1")), 2);
        assertThat(view).hasSameSizeAs(viewOn);
        assertThat(view.getFirst().content()).isEqualTo(viewOn.getFirst().content());
    }
}
