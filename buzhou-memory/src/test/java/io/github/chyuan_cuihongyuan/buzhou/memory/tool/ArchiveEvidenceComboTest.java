package io.github.chyuan_cuihongyuan.buzhou.memory.tool;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1109 / impl 861：归档×evidence 回查联动组合——归档（级联删除）后
 * 回查归档会话的 evidence 行为钉住 + 双读面各自守恒。纯测试轮。
 */
class ArchiveEvidenceComboTest {

    private BuzhouStores stores;
    private String sid;
    private String evidenceId;

    @BeforeEach
    void setUp() {
        EvidenceLookupTool.resetForTest();
        stores = Buzhou.inMemoryStores();
        sid = "arch-evi-sess";
        evidenceId = UUID.randomUUID().toString();
        stores.messageStore().append(sid, List.of(new BuzhouMessage(
                evidenceId, sid, 1, 0, Role.USER, "归档前证据",
                List.of(), null, null, null, Map.of(), Instant.now())));
    }

    @Test
    void archiveThenLookupCountsBothReadouts() {
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        EvidenceLookupTool lookup = new EvidenceLookupTool(stores.messageStore());

        // 归档前：回查命中
        assertThat(lookup.call("{\"evidenceId\":\"" + evidenceId + "\"}"))
                .isEqualTo("归档前证据");
        assertThat(EvidenceLookupTool.stats().hits()).isEqualTo(1);

        // 归档成功（级联删除活数据）
        assertThat(archiver.archive(sid)).isTrue();

        // 归档后：同 id 回查按 store 语义（数据已级联删除 → miss）
        String out = lookup.call("{\"evidenceId\":\"" + evidenceId + "\"}");
        assertThat(out).contains("未找到");

        SessionArchiver.ArchiveStats as = SessionArchiver.stats();
        assertThat(as.archiveCalls()).isEqualTo(1);
        assertThat(as.archived()).isEqualTo(1);
        EvidenceLookupTool.EvidenceLookupStats es = EvidenceLookupTool.stats();
        assertThat(es.calls()).isEqualTo(es.hits() + es.misses());
        assertThat(es.misses()).isEqualTo(1);
    }

    @Test
    void resetsAreIndependent() {
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        EvidenceLookupTool lookup = new EvidenceLookupTool(stores.messageStore());
        lookup.call("{\"evidenceId\":\"" + evidenceId + "\"}");

        SessionArchiver.resetForTest();
        assertThat(SessionArchiver.stats().archiveCalls()).isZero();

        EvidenceLookupTool.resetForTest();
        assertThat(EvidenceLookupTool.stats().calls()).isZero();
    }
}
