package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseAcquireResult;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionLeaseStore;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class JdbcSessionLeaseStore implements SessionLeaseStore {

    private final JdbcTemplate jdbc;

    private static final RowMapper<LeaseInfo> MAPPER = (rs, n) -> new LeaseInfo(
            rs.getString("owner_id"),
            rs.getLong("fencing_token"),
            rs.getTimestamp("acquired_at").toInstant(),
            rs.getTimestamp("expires_at").toInstant());

    public JdbcSessionLeaseStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public LeaseAcquireResult tryAcquire(String sessionId, String ownerId, Duration ttl) {
        Instant now = Instant.now();
        try {
            jdbc.update("""
                            INSERT INTO buzhou_session_lease
                            (session_id, owner_id, fencing_token, acquired_at, expires_at)
                            VALUES (?,?,?,?,?)
                            """,
                    sessionId, ownerId, 1L, Timestamp.from(now), Timestamp.from(now.plus(ttl)));
            return new LeaseAcquireResult(true, 1L);
        } catch (DuplicateKeyException ignored) {
        }
        Optional<LeaseInfo> existing = rawInspect(sessionId);
        if (existing.isPresent() && existing.get().expiresAt().isBefore(now)) {
            int updated = jdbc.update("""
                            UPDATE buzhou_session_lease
                            SET owner_id = ?, fencing_token = ?, acquired_at = ?, expires_at = ?
                            WHERE session_id = ? AND expires_at < ?
                            """,
                    ownerId, existing.get().fencingToken() + 1, Timestamp.from(now),
                    Timestamp.from(now.plus(ttl)), sessionId, Timestamp.from(now));
            if (updated == 1) {
                return new LeaseAcquireResult(true, existing.get().fencingToken() + 1);
            }
        }
        return new LeaseAcquireResult(false, existing.map(LeaseInfo::fencingToken).orElse(0L));
    }

    @Override
    public boolean renew(String sessionId, String ownerId, long fencingToken, Duration ttl) {
        return jdbc.update("""
                        UPDATE buzhou_session_lease SET expires_at = ?
                        WHERE session_id = ? AND owner_id = ? AND fencing_token = ?
                        """,
                Timestamp.from(Instant.now().plus(ttl)), sessionId, ownerId, fencingToken) == 1;
    }

    @Override
    public void release(String sessionId, String ownerId, long fencingToken) {
        jdbc.update("""
                        DELETE FROM buzhou_session_lease
                        WHERE session_id = ? AND owner_id = ? AND fencing_token = ?
                        """, sessionId, ownerId, fencingToken);
    }

    @Override
    public LeaseAcquireResult steal(String sessionId, String newOwnerId, Duration ttl) {
        Instant now = Instant.now();
        Optional<LeaseInfo> existing = rawInspect(sessionId);
        long newToken = existing.map(LeaseInfo::fencingToken).orElse(0L) + 1;
        if (existing.isPresent()) {
            jdbc.update("""
                            UPDATE buzhou_session_lease
                            SET owner_id = ?, fencing_token = ?, acquired_at = ?, expires_at = ?
                            WHERE session_id = ?
                            """,
                    newOwnerId, newToken, Timestamp.from(now), Timestamp.from(now.plus(ttl)),
                    sessionId);
        } else {
            jdbc.update("""
                            INSERT INTO buzhou_session_lease
                            (session_id, owner_id, fencing_token, acquired_at, expires_at)
                            VALUES (?,?,?,?,?)
                            """,
                    sessionId, newOwnerId, newToken, Timestamp.from(now),
                    Timestamp.from(now.plus(ttl)));
        }
        return new LeaseAcquireResult(true, newToken);
    }

    @Override
    public Optional<LeaseInfo> inspect(String sessionId) {
        return rawInspect(sessionId)
                .filter(info -> info.expiresAt().isAfter(Instant.now()));
    }

    private Optional<LeaseInfo> rawInspect(String sessionId) {
        return jdbc.query("SELECT * FROM buzhou_session_lease WHERE session_id = ?",
                MAPPER, sessionId).stream().findFirst();
    }
}
