package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 归档/还原每会话互斥测试（spec 622 / T894–T895 / impl 475）：同会话 archive 与
 * restore 串行化（交错=数据丢失窗关闭）、跨会话不互斥、并发同会话归档单成功。
 * 门/探针经 SessionCleaner.withContributor 钩子注入（final 类不可继承）。
 */
class SessionArchiverMutexTest {

    private static BuzhouStores storesWithSession(String sessionId) {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        var runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        var session = runtime.spawn("app", "agent", sessionId);
        session.chat("q");
        session.close();
        return stores;
    }

    /** 同会话并发双归档：恰一 true 一 false（第二个见空会话）；归档键唯一。 */
    @Test
    void concurrentArchiveSameSessionSingleWinner() throws Exception {
        BuzhouStores stores = storesWithSession("s-race");
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));

        CompletableFuture<Boolean> a = CompletableFuture.supplyAsync(() -> archiver.archive("s-race"));
        CompletableFuture<Boolean> b = CompletableFuture.supplyAsync(() -> archiver.archive("s-race"));

        long archived = a.thenCombine(b, (x, y) -> (x ? 1 : 0) + (y ? 1 : 0)).get(5, TimeUnit.SECONDS);
        assertThat(archived).isEqualTo(1);
        assertThat(archiver.archived()).containsExactly("s-race");
    }

    /** 跨会话归档真并行（spec 623：per-session 事务域——两会话 deleteSession 同时在飞）。 */
    @Test
    void differentSessionsArchiveInParallel() throws Exception {
        BuzhouStores stores = storesWithSession("s-1");
        stores.messageStore().append("s-2", List.of(
                new io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage(
                        java.util.UUID.randomUUID().toString(), "s-2", 1, 0,
                        io.github.chyuan_cuihongyuan.buzhou.core.message.Role.USER,
                        "q", List.of(), null, null, null, java.util.Map.of(), java.time.Instant.now())));
        CountDownLatch bothEntered = new CountDownLatch(2);
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        SessionCleaner counting = new SessionCleaner(stores).withContributor("parallel-probe", id -> {
            int now = inFlight.incrementAndGet();
            maxInFlight.accumulateAndGet(now, Math::max);
            bothEntered.countDown();
            try {
                bothEntered.await(3, TimeUnit.SECONDS); // 两边同时在钩子内 = 真并行
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                inFlight.decrementAndGet();
            }
        });
        SessionArchiver archiver = new SessionArchiver(stores, counting);

        CompletableFuture<Boolean> a = CompletableFuture.supplyAsync(() -> archiver.archive("s-1"));
        CompletableFuture<Boolean> b = CompletableFuture.supplyAsync(() -> archiver.archive("s-2"));

        assertThat(a.get(8, TimeUnit.SECONDS)).isTrue();
        assertThat(b.get(8, TimeUnit.SECONDS)).isTrue();
        assertThat(maxInFlight.get()).isEqualTo(2); // 全局锁时代恒 1——per-session 后并行
        assertThat(archiver.archived()).containsExactlyInAnyOrder("s-1", "s-2");
    }

    /** archive 进行中（级联删除被钩子阻塞）restore 同会话被互斥挡住：无数据丢失窗。 */
    @Test
    void restoreWaitsForInFlightArchiveOfSameSession() throws Exception {
        BuzhouStores stores = storesWithSession("s-ar");
        CountDownLatch deleteEntered = new CountDownLatch(1);
        CountDownLatch releaseDelete = new CountDownLatch(1);
        SessionCleaner gated = new SessionCleaner(stores).withContributor("gate", id -> {
            deleteEntered.countDown();
            try {
                releaseDelete.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        SessionArchiver archiver = new SessionArchiver(stores, gated);

        CompletableFuture<Boolean> archiving =
                CompletableFuture.supplyAsync(() -> archiver.archive("s-ar"));
        assertThat(deleteEntered.await(5, TimeUnit.SECONDS)).isTrue(); // archive 在 live-delete 中

        CompletableFuture<Boolean> restoring =
                CompletableFuture.supplyAsync(() -> archiver.restore("s-ar"));
        Thread.sleep(200);
        assertThat(restoring.isDone()).isFalse(); // 被同会话互斥挡住（archive 未完）

        releaseDelete.countDown();
        assertThat(archiving.get(5, TimeUnit.SECONDS)).isTrue();
        // archive 完成后 restore 才进：归档键在 → 还原成功（无互斥时 restore 可能在 archive
        // 删活数据前读走归档键、随后被 archive 删掉刚还原的数据——丢失窗）
        assertThat(restoring.get(5, TimeUnit.SECONDS)).isTrue();
        assertThat(stores.messageStore().load("s-ar")).isNotEmpty();
    }
}
