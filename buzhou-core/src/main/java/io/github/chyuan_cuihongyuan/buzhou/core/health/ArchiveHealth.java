package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;

import java.util.Map;

/**
 * 会话归档健康面（spec 102 §A / T379，spec 97 fog 项收口）：恒 UP（观测面——
 * 归档多不是故障，容量治理走运维）；details = 归档在册数（countByPrefix 下推，
 * 零值读）。无归档 = 0（合法状态——归档未启用不报 UNKNOWN：与 error-signatures
 * 同为观测段，非机制启用态）。
 */
public final class ArchiveHealth implements BuzhouHealth {

    private final SessionStateStore stateStore;

    public ArchiveHealth(SessionStateStore stateStore) {
        this.stateStore = stateStore;
    }

    @Override
    public String mechanism() {
        return "session-archive";
    }

    @Override
    public Status status() {
        return Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        return Map.of("archivedSessions",
                stateStore.countByPrefix(
                        io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver.ARCHIVE_SESSION_ID,
                        io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver.ARCHIVE_PREFIX));
    }
}
