package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SummaryStore;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcSummaryStore implements SummaryStore {

    /**
     * 版本生成并发重试上限（spec 13 §stores-7 / ticket 32）：乐观轨撞唯一索引后重取新版本号，
     * 双线程紧环压测本地实测最大重试 10 次（见 H2SummaryConcurrencyTest），32 留 3 倍余量。
     */
    static final int MAX_VERSION_ATTEMPTS = 32;

    /** 版本号起始值（空会话首插，无行可锁窗口的兜底即在此处）。 */
    private static final long FIRST_VERSION = 1L;

    /** H2 乐观轨：取当前最大版本（无锁，读-插窗口由唯一索引 + 重试兜底）。 */
    private static final String MAX_VERSION_SQL =
            "SELECT COALESCE(MAX(version), 0) FROM buzhou_summary WHERE session_id = ?";

    /**
     * MySQL/PG 悲观轨：锁住会话当前最大版本行（InnoDB/PG 锁定读在锁等待结束后读最新已提交行，
     * 天然序列化同会话写者；首插无行可锁窗口由唯一索引 + 重试兜底）。
     */
    private static final String MAX_VERSION_FOR_UPDATE_SQL =
            "SELECT version FROM buzhou_summary WHERE session_id = ? "
                    + "ORDER BY version DESC LIMIT 1 FOR UPDATE";

    private static final String INSERT_SQL = """
            INSERT INTO buzhou_summary (session_id, version, sections, token_estimate, created_at)
            VALUES (?,?,?,?,?)
            """;

    private final JdbcTemplate jdbc;

    /** SQL 方言（版本生成轨道选择）。 */
    private final Dialect dialect;

    /** 共享事务模板（锁读 + 插入必须同事务）：null = 兼容旧自动提交路径（保留旧竞态）。 */
    @Nullable
    private final TransactionTemplate transactionTemplate;

    private static final RowMapper<StructuredSummary> MAPPER = (rs, n) -> new StructuredSummary(
            rs.getString("session_id"),
            rs.getLong("version"),
            JdbcJson.readMap(rs.getString("sections")).entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey, e -> String.valueOf(e.getValue()))),
            rs.getInt("token_estimate"),
            rs.getTimestamp("created_at").toInstant());

    /** 兼容旧构造器：H2 乐观轨（全方言正确——唯一索引 + 重试兜底）、无事务模板。 */
    public JdbcSummaryStore(JdbcTemplate jdbc) {
        this(jdbc, Dialect.H2, null);
    }

    public JdbcSummaryStore(JdbcTemplate jdbc, Dialect dialect,
                            @Nullable TransactionTemplate transactionTemplate) {
        this.jdbc = jdbc;
        this.dialect = dialect == null ? Dialect.H2 : dialect;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 保存摘要并返回其版本号（ticket 32：原子化，消灭 {@code MAX(version)+1} 读改写竞态）。
     *
     * <p><b>方言分轨</b>（任务书建议的 PG/H2 {@code ON CONFLICT} 形状经本地实测不可行：
     * H2 2.4.240 不支持 {@code ON CONFLICT} / {@code ON DUPLICATE KEY}（MySQL 兼容模式专属），
     * 且 upsert 形状无法可靠取回「本事务实际插入的版本号」）：
     * <ul>
     *   <li><b>H2 = 乐观轨</b>：无锁读 max+1 后直接插入。H2 的 FOR UPDATE 在锁等待结束后
     *       仍返回等待前的旧快照（MVStore 行为，本地实测）——锁定读反而制造「等待→读到旧值→
     *       必冲突」的乒乓活锁，故 H2 走乐观 + 撞唯一索引重试（重试重读必见最新已提交值，
     *       收敛有保障）；</li>
     *   <li><b>MySQL / PG = 悲观轨</b>：{@code SELECT ... LIMIT 1 FOR UPDATE} 锁住当前最大
     *       版本行序列化同会话写者（InnoDB/PG 锁定读等待后读最新已提交行），插入冲突
     *       （首插窗口无行可锁）仅剩罕见残余，重试兜底。</li>
     * </ul>
     */
    @Override
    public long save(String sessionId, StructuredSummary summary) {
        for (int attempt = 1; attempt <= MAX_VERSION_ATTEMPTS; attempt++) {
            try {
                return JdbcTransactions.inCurrentOrNew(transactionTemplate,
                        () -> saveWithNextVersion(sessionId, summary));
            } catch (DuplicateKeyException e) {
                // 并发窗口撞唯一索引 idx_summary_session_version——重取新版本号重试
            }
        }
        throw new BuzhouException(ErrorCode.STORE_WRITE_FAILED,
                "摘要版本生成并发冲突重试耗尽(sessionId=%s, attempts=%d)"
                        .formatted(sessionId, MAX_VERSION_ATTEMPTS));
    }

    /** 取下一版本 + 插入（悲观轨必须在事务内调用，锁与插入同进退）。 */
    private long saveWithNextVersion(String sessionId, StructuredSummary summary) {
        long current = dialect == Dialect.H2
                ? jdbc.queryForObject(MAX_VERSION_SQL, Long.class, sessionId)
                : jdbc.query(MAX_VERSION_FOR_UPDATE_SQL, (rs, n) -> rs.getLong(1), sessionId)
                        .stream().findFirst().orElse(0L);
        long next = current + FIRST_VERSION;
        jdbc.update(INSERT_SQL,
                sessionId, next, JdbcJson.write(summary.sections()),
                summary.tokenEstimate(), Timestamp.from(summary.createdAt()));
        return next;
    }

    @Override
    public Optional<StructuredSummary> latest(String sessionId) {
        return jdbc.query("""
                        SELECT * FROM buzhou_summary WHERE session_id = ?
                        ORDER BY version DESC LIMIT 1
                        """, MAPPER, sessionId).stream().findFirst();
    }

    @Override
    public List<StructuredSummary> history(String sessionId, int limit) {
        return jdbc.query("""
                        SELECT * FROM buzhou_summary WHERE session_id = ?
                        ORDER BY version DESC LIMIT ?
                        """, MAPPER, sessionId, limit);
    }

    /** impl-35 / spec 13 §stores-6：单表批量删（幂等；单语句自原子）。 */
    @Override
    public void deleteSession(String sessionId) {
        jdbc.update("DELETE FROM buzhou_summary WHERE session_id = ?", sessionId);
    }

    /** impl-37 / spec 13 §stores-6：旧版本修剪——每会话保留最近 keepLatest 个版本（etcd compaction 语义）。 */
    @Override
    public int pruneVersions(int keepLatest) {
        int keep = Math.max(1, keepLatest);
        int deleted = 0;
        for (String sessionId : jdbc.queryForList(
                "SELECT DISTINCT session_id FROM buzhou_summary", String.class)) {
            deleted += jdbc.update("""
                            DELETE FROM buzhou_summary WHERE session_id = ? AND version <= (
                              SELECT max_version FROM (SELECT MAX(version) - ? AS max_version
                                FROM buzhou_summary WHERE session_id = ?) AS mv)
                            """,
                    sessionId, keep, sessionId);
        }
        return deleted;
    }
}
