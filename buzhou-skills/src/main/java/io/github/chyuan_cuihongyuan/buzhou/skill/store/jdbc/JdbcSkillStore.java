package io.github.chyuan_cuihongyuan.buzhou.skill.store.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillStatus;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.DbSkillRecord;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.DbSkillResourceRecord;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillStore;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillVersionConflictException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * {@link SkillStore} 的 JDBC 实现（impl-51 / spec 14 §G）。
 *
 * <p><b>放置决策</b>：SkillStore 是 buzhou-skills 自有 SPI（store-* 模块只依赖 core，
 * 星形拓扑不许 store-jdbc 反向实现 feature SPI）——实现托管在 skills 模块内，
 * spring-boot-starter-jdbc 以 optional 依赖进入（无 jdbc 运行时零影响）。
 *
 * <p><b>DDL 自含</b>：首用 {@code CREATE TABLE IF NOT EXISTS}（H2/MySQL/PostgreSQL 一致语法），
 * 不耦合 store-jdbc 的 V<n> 迁移轨道（表归属 feature 模块；版本化迁移注记为后续项）。
 *
 * <p>乐观锁：UPDATE ... WHERE version=? 0 行命中即 {@link SkillVersionConflictException}。
 */
public class JdbcSkillStore implements SkillStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS buzhou_skill (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                name        VARCHAR(128)  NOT NULL UNIQUE,
                description VARCHAR(1024),
                allowed_tools CLOB,
                body        CLOB,
                status      VARCHAR(16)   NOT NULL,
                created_by  VARCHAR(128),
                created_at  TIMESTAMP     NOT NULL,
                updated_at  TIMESTAMP     NOT NULL,
                version     INT           NOT NULL
            )""";
    private static final String DDL_RESOURCE = """
            CREATE TABLE IF NOT EXISTS buzhou_skill_resource (
                id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                skill_name    VARCHAR(128) NOT NULL,
                relative_path VARCHAR(512) NOT NULL,
                media_type    VARCHAR(128),
                content       CLOB,
                size_bytes    BIGINT       NOT NULL,
                updated_at    TIMESTAMP    NOT NULL,
                UNIQUE (skill_name, relative_path)
            )""";

    private static final RowMapper<DbSkillRecord> SKILL_MAPPER = (rs, n) -> {
        List<String> tools = parseTools(rs.getString("allowed_tools"));
        return new DbSkillRecord(
                rs.getLong("id"), rs.getString("name"), rs.getString("description"), tools,
                rs.getString("body"), SkillStatus.valueOf(rs.getString("status")),
                rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getInt("version"));
    };

    private static final RowMapper<DbSkillResourceRecord> RESOURCE_MAPPER = (rs, n) ->
            new DbSkillResourceRecord(rs.getLong("id"), rs.getString("skill_name"),
                    rs.getString("relative_path"), rs.getString("media_type"),
                    rs.getString("content"), rs.getLong("size_bytes"),
                    rs.getTimestamp("updated_at").toInstant());

    private final JdbcTemplate jdbc;
    private volatile boolean schemaReady;

    public JdbcSkillStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private void ensureSchema() {
        if (!schemaReady) {
            synchronized (this) {
                if (!schemaReady) {
                    jdbc.execute(DDL);
                    jdbc.execute(DDL_RESOURCE);
                    schemaReady = true;
                }
            }
        }
    }

    // ---- Skill 主表 ----

    @Override
    public Optional<DbSkillRecord> findByName(String name) {
        ensureSchema();
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT * FROM buzhou_skill WHERE name = ?", SKILL_MAPPER, name));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<DbSkillRecord> findPublished(String name) {
        return findByName(name).filter(r -> r.status() == SkillStatus.PUBLISHED);
    }

    @Override
    public List<DbSkillRecord> findAll() {
        ensureSchema();
        return jdbc.query("SELECT * FROM buzhou_skill ORDER BY name", SKILL_MAPPER);
    }

    @Override
    public DbSkillRecord save(DbSkillRecord record) {
        ensureSchema();
        if (record.id() == null || record.id() == 0) {
            // 新建（name 冲突 = 并发新建，转乐观锁异常）
            Optional<DbSkillRecord> existing = findByName(record.name());
            if (existing.isPresent()) {
                throw new SkillVersionConflictException(record.name(), record.version(),
                        existing.get().version());
            }
            Instant now = Instant.now();
            jdbc.update("INSERT INTO buzhou_skill (name, description, allowed_tools, body, status, "
                            + "created_by, created_at, updated_at, version) VALUES (?,?,?,?,?,?,?,?,?)",
                    record.name(), record.description(), writeTools(record.allowedTools()),
                    record.body(), record.status().name(), record.createdBy(),
                    Timestamp.from(now), Timestamp.from(now), 1);
            return findByName(record.name()).orElseThrow();
        }
        // 更新：乐观锁（version 不匹配 0 行命中）
        int updated = jdbc.update("UPDATE buzhou_skill SET description = ?, allowed_tools = ?, "
                        + "body = ?, status = ?, updated_at = ?, version = version + 1 "
                        + "WHERE id = ? AND version = ?",
                record.description(), writeTools(record.allowedTools()), record.body(),
                record.status().name(), Timestamp.from(Instant.now()), record.id(), record.version());
        if (updated == 0) {
            int currentVersion = findByName(record.name()).map(DbSkillRecord::version).orElse(-1);
            throw new SkillVersionConflictException(record.name(), record.version(), currentVersion);
        }
        return findByName(record.name()).orElseThrow();
    }

    @Override
    public boolean deleteByName(String name) {
        ensureSchema();
        deleteResources(name);
        return jdbc.update("DELETE FROM buzhou_skill WHERE name = ?", name) > 0;
    }

    // ---- 资源表 ----

    @Override
    public Optional<DbSkillResourceRecord> findResource(String skillName, String relativePath) {
        ensureSchema();
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT * FROM buzhou_skill_resource WHERE skill_name = ? AND relative_path = ?",
                    RESOURCE_MAPPER, skillName, relativePath));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<DbSkillResourceRecord> findResources(String skillName) {
        ensureSchema();
        return jdbc.query(
                "SELECT * FROM buzhou_skill_resource WHERE skill_name = ? ORDER BY relative_path",
                RESOURCE_MAPPER, skillName);
    }

    @Override
    public DbSkillResourceRecord saveResource(DbSkillResourceRecord record) {
        ensureSchema();
        Instant now = Instant.now();
        // upsert（唯一键 skill_name+relative_path）：先删后插，语义最直白且跨方言
        jdbc.update("DELETE FROM buzhou_skill_resource WHERE skill_name = ? AND relative_path = ?",
                record.skillName(), record.relativePath());
        jdbc.update("INSERT INTO buzhou_skill_resource (skill_name, relative_path, media_type, "
                        + "content, size_bytes, updated_at) VALUES (?,?,?,?,?,?)",
                record.skillName(), record.relativePath(), record.mediaType(), record.content(),
                record.sizeBytes(), Timestamp.from(now));
        return findResource(record.skillName(), record.relativePath()).orElseThrow();
    }

    @Override
    public void deleteResources(String skillName) {
        ensureSchema();
        jdbc.update("DELETE FROM buzhou_skill_resource WHERE skill_name = ?", skillName);
    }

    // ---- JSON 工具序列化 ----

    private static String writeTools(List<String> tools) {
        try {
            return MAPPER.writeValueAsString(tools == null ? List.of() : tools);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("allowed-tools 序列化失败", e);
        }
    }

    private static List<String> parseTools(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return List.of();
        }
    }
}
