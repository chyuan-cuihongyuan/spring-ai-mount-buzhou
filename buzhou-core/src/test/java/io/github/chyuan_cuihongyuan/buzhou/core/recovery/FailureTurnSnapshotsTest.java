package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 523 / T797–798：失败轮快照——onTurnStart 记输入 + onTurnError 落
 * 快照（错误类/消息截断/输入预览截断）、环形有界、成功轮不入、JSONL 导出、
 * 会话 E2E（ScriptedChatModel 抛错）。
 */
class FailureTurnSnapshotsTest {

    @Test
    void errorTurnCapturesSnapshotWithTruncation() {
        FailureTurnSnapshots snapshots = new FailureTurnSnapshots(16, 20);
        snapshots.onTurnStart(1, "x".repeat(50));
        snapshots.onTurnError(1, new IllegalStateException("boom: " + "y".repeat(300)));

        assertThat(snapshots.snapshot()).hasSize(1);
        var snap = snapshots.snapshot().getFirst();
        assertThat(snap.turnSeq()).isEqualTo(1);
        assertThat(snap.errorClass()).isEqualTo(IllegalStateException.class.getName());
        assertThat(snap.errorMessage()).hasSize(256); // 错误消息截断
        assertThat(snap.inputPreview()).hasSize(21); // 20 字符 + 省略号
        assertThat(snapshots.totalErrors()).isEqualTo(1);
    }

    @Test
    void successfulTurnsNeverCapturedAndRingBounded() {
        FailureTurnSnapshots snapshots = new FailureTurnSnapshots(8, 64);
        snapshots.onTurnStart(1, "ok input");
        snapshots.onTurnEnd(1, "reply"); // 成功轮不入
        assertThat(snapshots.snapshot()).isEmpty();

        for (int i = 1; i <= 20; i++) {
            snapshots.onTurnStart(i, "in" + i);
            snapshots.onTurnError(i, new RuntimeException("e" + i));
        }
        assertThat(snapshots.snapshot()).hasSize(8); // 环形有界
        assertThat(snapshots.totalErrors()).isEqualTo(20); // 环外总量
        assertThat(snapshots.snapshot().getFirst().turnSeq()).isEqualTo(13); // 旧→新
    }

    @Test
    void unknownTurnErrorHasEmptyInput() {
        FailureTurnSnapshots snapshots = new FailureTurnSnapshots();
        snapshots.onTurnError(99, new RuntimeException("no start"));
        assertThat(snapshots.snapshot().getFirst().inputPreview()).isEmpty();
    }

    @Test
    void jsonlExportOneLinePerSnapshot() throws Exception {
        FailureTurnSnapshots snapshots = new FailureTurnSnapshots();
        snapshots.onTurnStart(1, "输入\"带引号\"\n换行");
        snapshots.onTurnError(1, new RuntimeException("failed"));
        StringWriter out = new StringWriter();
        long lines = snapshots.exportJsonl(out);
        assertThat(lines).isEqualTo(1);
        String json = out.toString();
        assertThat(json).contains("\"errorClass\":\"java.lang.RuntimeException\"");
        assertThat(json).contains("failed");
        assertThat(json).doesNotContain("\n\""); // 引号换行已转义——单行 JSONL
    }

    @Test
    void sessionE2EErrorTurnSnapshotCaptured() {
        var model = new io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel();
        model.enqueueThrow(new RuntimeException("模型爆炸"));
        FailureTurnSnapshots snapshots = new FailureTurnSnapshots();
        var runtime = io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.runtime(
                model, io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores(),
                new io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig(
                        java.util.List.of(), java.util.Set.of(), java.util.Set.of(), null,
                        java.util.List.of(), java.util.Map.of(), java.util.List.of(),
                        java.util.List.of(ctx -> ctx.addObserver(snapshots)), null));
        var session = runtime.spawn("app", "agent", "sess-err");
        try {
            session.chat("触发失败");
        } catch (RuntimeException ignored) {
            // 预期失败轮
        }
        session.close();
        assertThat(snapshots.snapshot()).hasSize(1);
        assertThat(snapshots.snapshot().getFirst().errorMessage()).contains("模型爆炸");
    }
}
