package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1524 / T2299–T2300：配置错误显形双小项——
 * ① HookChain 重复 hook 名 WARN（派发序不稳定/对位歧义信号）；
 * ② SessionAssemblyContext.addObserver 同实例重复注册去重（双份通知是装配错误）。
 */
class HookDuplicateNameWarnTest {

    /** 同名双 hook（order 同）——链可构建、WARN 显形（不炸装配）。 */
    @Test
    void duplicateHookNamesShouldBuildChainAndWarn() {
        BuzhouHook a = noop("dup-hook");
        BuzhouHook b = noop("dup-hook");
        HookChain chain = HookChain.of(List.of(a, b));
        assertThat(chain.hooks()).hasSize(2); // 不炸装配（治理归 WARN + 调用方决断）
        assertThat(chain.composition().resolvedHookNames()).containsExactly("dup-hook", "dup-hook");
    }

    /** 同一 observer 实例重复注册去重——单份通知。 */
    @Test
    void duplicateObserverRegistrationShouldDedupe() {
        RecordingObserver observer = new RecordingObserver();
        List<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer> customizers = List.of(
                ctx -> ctx.addObserver(observer),
                ctx -> ctx.addObserver(observer)); // 同实例二次注册
        RuntimeConfig config = new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                null, List.of(), Map.of(), List.of(), customizers, null);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("ok");

        try (var agent = Buzhou.runtime(model, Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-dup-obs")) {
            agent.chat("q");
            // 单份通知（open + start + end 各恰一次）
            assertThat(observer.events).containsExactly("open", "start:1", "end:1");
        }
    }

    private static BuzhouHook noop(String name) {
        return new BuzhouHook() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public HookResult beforeTurn(TurnContext ctx) {
                return HookResult.CONTINUE;
            }
        };
    }

    private static final class RecordingObserver
            implements io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver {
        final List<String> events = new CopyOnWriteArrayList<>();

        @Override
        public void onOpen() {
            events.add("open");
        }

        @Override
        public void onTurnStart(int turnSeq, String userInput) {
            events.add("start:" + turnSeq);
        }

        @Override
        public void onTurnEnd(int turnSeq, String finalReply) {
            events.add("end:" + turnSeq);
        }
    }
}
