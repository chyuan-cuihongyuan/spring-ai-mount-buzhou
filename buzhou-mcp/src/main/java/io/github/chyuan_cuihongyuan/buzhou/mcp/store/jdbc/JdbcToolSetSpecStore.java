package io.github.chyuan_cuihongyuan.buzhou.mcp.store.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.mcp.ToolSetSpecStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * {@link ToolSetSpecStore} 的 JDBC 实现（impl-51 / spec 14 §G）。
 *
 * <p>表结构：KV 载体（spec 04「DB 数据源复用 05 配置通道的 KV 载体」）——
 * {@code buzhou_mcp_toolset(name PK, spec_json CLOB, updated_at)}。
 * DDL 自含（{@code IF NOT EXISTS}，与 JdbcSkillStore 同决策：feature SPI 实现托管 feature 模块，
 * optional jdbc 依赖，不耦合 store-jdbc 迁移轨道）。
 *
 * <p>写入面（运维/管理侧替换清单）经 {@link #replaceAll}——同 {@code InMemoryToolSetSpecStore}
 * 的整表替换语义。
 */
public class JdbcToolSetSpecStore implements ToolSetSpecStore {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS buzhou_mcp_toolset (
                name       VARCHAR(128) PRIMARY KEY,
                spec_json  CLOB         NOT NULL,
                updated_at TIMESTAMP    NOT NULL
            )""";

    private final JdbcTemplate jdbc;
    private volatile boolean schemaReady;

    public JdbcToolSetSpecStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private void ensureSchema() {
        if (!schemaReady) {
            synchronized (this) {
                if (!schemaReady) {
                    jdbc.execute(DDL);
                    schemaReady = true;
                }
            }
        }
    }

    @Override
    public List<ToolSetSpec> loadAll() {
        ensureSchema();
        return jdbc.query("SELECT spec_json FROM buzhou_mcp_toolset ORDER BY name",
                (rs, n) -> read(rs.getString("spec_json")));
    }

    /** 整表替换（运维侧写入口）：事务内删全表 + 批量插入。 */
    public void replaceAll(List<ToolSetSpec> specs) {
        ensureSchema();
        jdbc.update("DELETE FROM buzhou_mcp_toolset");
        java.sql.Timestamp now = java.sql.Timestamp.from(java.time.Instant.now());
        for (ToolSetSpec spec : specs) {
            jdbc.update("INSERT INTO buzhou_mcp_toolset (name, spec_json, updated_at) VALUES (?,?,?)",
                    spec.name(), write(spec), now);
        }
    }

    private static String write(ToolSetSpec spec) {
        try {
            return MAPPER.writeValueAsString(spec);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("ToolSetSpec 序列化失败：" + spec.name(), e);
        }
    }

    private static ToolSetSpec read(String json) {
        try {
            return MAPPER.readValue(json, ToolSetSpec.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("ToolSetSpec 反序列化失败", e);
        }
    }
}
