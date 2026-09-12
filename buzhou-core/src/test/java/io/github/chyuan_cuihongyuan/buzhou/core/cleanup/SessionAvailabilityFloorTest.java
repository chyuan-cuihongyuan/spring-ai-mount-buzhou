package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话最小可用水位闸测试（spec 704 / T959–T960 / impl 507）：恰在 floor 拒绝
 * + 计数、之上放行真归档、未知（负数）fail-open、restore 不受闸、null floor
 * 零回归。
 */
class SessionAvailabilityFloorTest {

    private static BuzhouMessage message(String sid, int turn, String text) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sid, turn, 0,
                io.github.chyuan_cuihongyuan.buzhou.core.message.Role.USER, text,
                List.of(), null, null, null, Map.of(), Instant.now());
    }

    /** 造一个可归档的非空会话，返回 sid。 */
    private static String seedSession(BuzhouStores stores, String sid) {
        stores.messageStore().append(sid, List.of(message(sid, 1, "问"), message(sid, 2, "再问")));
        stores.summaryStore().save(sid, new StructuredSummary(sid, 3,
                Map.of("intent", "调优"), 42, Instant.now()));
        return sid;
    }

    @Test
    void floorRejectsArchiveAtThresholdAndCounts() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = seedSession(stores, "s-pdb-1");
        AtomicInteger live = new AtomicInteger(2);
        SessionAvailabilityFloor floor = new SessionAvailabilityFloor(2, live::get);
        SessionArchiver archiver = new SessionArchiver(stores,
                new SessionCleaner(stores), floor);

        assertThat(archiver.archive(sid)).isFalse(); // 2 live <= minAvailable 2——拒绝
        assertThat(archiver.archived()).isEmpty();   // 归档键未产生
        assertThat(stores.messageStore().load(sid)).isNotEmpty(); // 活数据未动
    }

    @Test
    void aboveFloorArchivesNormally() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = seedSession(stores, "s-pdb-2");
        AtomicInteger live = new AtomicInteger(3);
        SessionAvailabilityFloor floor = new SessionAvailabilityFloor(2, live::get);
        SessionArchiver archiver = new SessionArchiver(stores,
                new SessionCleaner(stores), floor);

        assertThat(archiver.archive(sid)).isTrue(); // 3 > 2——放行
        assertThat(archiver.archived()).containsExactly(sid);
    }

    @Test
    void unknownCountFailsOpen() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = seedSession(stores, "s-pdb-3");
        // 计数源缺席（-1）——fail-open 放行（保底闸失明不误伤运维动作）
        SessionAvailabilityFloor floor = new SessionAvailabilityFloor(5, () -> -1);
        SessionArchiver archiver = new SessionArchiver(stores,
                new SessionCleaner(stores), floor);

        assertThat(archiver.archive(sid)).isTrue();
    }

    @Test
    void restoreNotGated() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = seedSession(stores, "s-pdb-4");
        SessionAvailabilityFloor floor = new SessionAvailabilityFloor(9, () -> 0);
        SessionArchiver archiver = new SessionArchiver(stores,
                new SessionCleaner(stores), floor);
        assertThat(archiver.archive(sid)).isFalse(); // 0 <= 9 拒绝

        assertThat(archiver.restore(sid)).isFalse(); // 无归档键——但闸本身不拦 restore 语义
        // 既有归档还原不受闸（用无闸实例直接归档后，有闸实例可还原）
        SessionArchiver ungated = new SessionArchiver(stores, new SessionCleaner(stores));
        assertThat(ungated.archive(sid)).isTrue();
        assertThat(archiver.restore(sid)).isTrue(); // 有闸实例还原放行
    }

    @Test
    void nullFloorKeepsLegacyBehavior() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = seedSession(stores, "s-pdb-5");
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        assertThat(archiver.archive(sid)).isTrue(); // 默认无闸——零变化
    }

    @Test
    void negativeMinAvailableRejected() {
        AtomicInteger live = new AtomicInteger(1);
        assertThat(new SessionAvailabilityFloor(0, live::get).allowsArchive()).isTrue();
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> new SessionAvailabilityFloor(-1, live::get))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
