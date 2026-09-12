package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PDB×空闲压缩联动补验（spec 734 / T1019–T1020 / impl 537）：sweep 空闲候选
 * 在水位不足时被 floor 逐个拒绝（计数一致）；水位恢复后放行归档。
 */
class SessionAvailabilityFloorIdleE2ETest {

    private static void seedSession(BuzhouStores stores, String sid) {
        stores.messageStore().append(sid, List.of(new BuzhouMessage(
                UUID.randomUUID().toString(), sid, 1, 0, Role.USER, "问",
                List.of(), null, null, null, Map.of(), Instant.now())));
    }

    @Test
    void idleCandidatesDeniedThenAllowedAsWaterRecovers() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        for (int i = 0; i < 4; i++) {
            seedSession(stores, "s-idle-" + i);
        }
        AtomicInteger live = new AtomicInteger(2); // 水位：仅 2 存活（阈值 2）
        SessionAvailabilityFloor floor = new SessionAvailabilityFloor(2, live::get);
        SessionArchiver archiver = new SessionArchiver(stores,
                new io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner(stores), floor);

        // 4 个空闲候选逐个尝试——全部被闸拒绝
        for (int i = 0; i < 4; i++) {
            assertThat(archiver.archive("s-idle-" + i)).isFalse();
        }
        assertThat(archiver.archived()).isEmpty();

        // 水位恢复（3 个会话回流在线——live 升至 5）
        live.set(5);
        assertThat(archiver.archive("s-idle-0")).isTrue();
        assertThat(archiver.archived()).containsExactly("s-idle-0");
    }
}
