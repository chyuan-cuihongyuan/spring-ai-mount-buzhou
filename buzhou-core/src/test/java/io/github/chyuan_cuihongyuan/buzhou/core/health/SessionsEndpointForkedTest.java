package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * fork 谱系面板段测试（spec 627 / T904–T905 / impl 480）：fork 分支被计入 forkedActive、
 * 非分支不计、state 读面缺席诚实 available=false；谱系键与 DefaultAgentRuntime 写入
 * 双向钉住（真实 fork → 面板可见）。
 */
class SessionsEndpointForkedTest {

    private static io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnAdmissionFloor floor() {
        return new io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnAdmissionFloor();
    }

    private static io.github.chyuan_cuihongyuan.buzhou.core.backpressure.MaintenanceCordon cordon(
            io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnAdmissionFloor floor) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.backpressure.MaintenanceCordon(
                floor, null, null, "", java.time.Duration.ofSeconds(15), java.time.Clock.systemUTC());
    }

    /** 真实 fork → 活跃分支计入 forkedActive；非分支会话不计。 */
    @Test
    @SuppressWarnings("unchecked")
    void forkedBranchCountedInDashboard() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a1");
        model.enqueueText("a2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        var runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        var source = runtime.spawn("app", "agent", "sess-src");
        source.chat("q");
        source.close();
        runtime.fork("sess-src", "app", "agent", "sess-branch"); // 分支活跃（未 close）

        var index = new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionIndexStore();
        // 索引按活跃登记（spec 30 生命周期维护——测试侧手工登记两会话）
        index.upsert(new io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo(
                "sess-branch", "app", "agent",
                io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo.STATUS_ACTIVE,
                java.time.Instant.now().toEpochMilli(), java.time.Instant.now().toEpochMilli(),
                1, java.util.Map.of()));
        index.upsert(new io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo(
                "sess-src", "app", "agent",
                io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo.STATUS_ACTIVE,
                java.time.Instant.now().toEpochMilli(), java.time.Instant.now().toEpochMilli(),
                1, java.util.Map.of()));

        var floor = floor();
        var endpoint = new BuzhouSessionsEndpoint(index, floor, cordon(floor), 16,
                stores.sessionStateStore());
        Map<String, Object> dashboard = endpoint.sessionsDashboard();

        Map<String, Object> forked = (Map<String, Object>) dashboard.get("forkedActive");
        assertThat(forked).containsEntry("available", true);
        assertThat(forked).containsEntry("count", 1L); // 仅分支（源无谱系键）
    }

    /** state 读面缺席（四参构造）→ 段诚实 available=false。 */
    @Test
    @SuppressWarnings("unchecked")
    void absentStateStoreReportsUnavailable() {
        var index = new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionIndexStore();
        var floor = floor();
        var endpoint = new BuzhouSessionsEndpoint(index, floor, cordon(floor));

        Map<String, Object> forked =
                (Map<String, Object>) endpoint.sessionsDashboard().get("forkedActive");
        assertThat(forked).containsEntry("available", false);
    }
}
