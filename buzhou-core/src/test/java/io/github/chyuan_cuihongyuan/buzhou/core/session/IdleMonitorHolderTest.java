package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 空闲监控 Holder 接线测试（spec 1620 / T2391–T2392 / impl 1173）：
 * 特征仓→监控器→直方全链（spec 161/179/841 三孤类接线）——sweep 判定空闲
 * 超阈值会话 + 翻转通知 + 直方入账；hook 节拍驱动 sweep。
 */
class IdleMonitorHolderTest {

    @Test
    void fullChainSweepsIdleSessionsAndFeedsHistogram() {
        SessionFeatureStore store = new SessionFeatureStore();
        IdleSessionMonitor monitor = new IdleSessionMonitor(store, java.time.Duration.ofMinutes(15));
        IdleDurationHistogram histogram = new IdleDurationHistogram();
        AtomicBoolean idleEntered = new AtomicBoolean();
        monitor.onChange((sessionId, entered) -> {
            if (entered) {
                idleEntered.set(true);
            }
        });

        store.recordTurnStart("s-idle");
        store.recordTurnStart("s-fresh");
        Instant freshMoment = Instant.now();
        // 两会话都已 lastActiveAt=now；判定时 s-idle 超 16 分钟空闲——用同一 store 语义验证：
        // 直接以未来时刻 sweep（两会话都空闲 16+ 分钟），翻转通知对两者都发生
        List<IdleSessionMonitor.IdleInfo> idle = monitor.sweep(freshMoment.plusSeconds(16 * 60 + 1));
        assertThat(idle).extracting(IdleSessionMonitor.IdleInfo::sessionId)
                .containsExactlyInAnyOrder("s-idle", "s-fresh");
        assertThat(idleEntered.get()).isTrue();
        for (IdleSessionMonitor.IdleInfo info : idle) {
            histogram.record(info.idleMillis());
        }
        assertThat(histogram.total()).isEqualTo(2);
        assertThat(java.util.Arrays.stream(histogram.bucketCounts()).sum()).isEqualTo(2);
    }

    @Test
    void sweepAndRecordConvenienceFeedsHolderHistogram() {
        // Holder 静态面：真实 lastActiveAt（Instant.now()），16 分钟后 sweep 应判空闲
        IdleMonitorHolder.store().recordTurnStart("holder-session");
        List<IdleSessionMonitor.IdleInfo> idle =
                IdleMonitorHolder.sweepAndRecord(Instant.now().plusSeconds(16 * 60));
        assertThat(idle).extracting(IdleSessionMonitor.IdleInfo::sessionId)
                .contains("holder-session");
        assertThat(IdleMonitorHolder.histogram().total()).isPositive();
    }

    @Test
    void sessionFeaturesHookFeedsHolderStore() {
        SessionFeaturesHook hook = new SessionFeaturesHook(null); // null → Holder store
        io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext ctx =
                new io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext() {
                    @Override
                    public String sessionId() {
                        return "hook-fed";
                    }

                    @Override
                    public String input() {
                        return "";
                    }

                    @Override
                    public String response() {
                        return "";
                    }

                    @Override
                    public void replaceInput(String newInput) {
                    }

                    @Override
                    public void replaceResponse(String newResponse) {
                    }

                    @Override
                    public String agentName() {
                        return "agent";
                    }

                    @Override
                    public int turn() {
                        return 1;
                    }

                    @Override
                    public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle state() {
                        return null;
                    }

                    @Override
                    public void emitEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent event) {
                    }
                };
        hook.beforeTurn(ctx);
        assertThat(IdleMonitorHolder.store().features("hook-fed").turns()).isEqualTo(1);
    }
}
