package io.github.chyuan_cuihongyuan.buzhou.skill.store.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillStatus;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.DbSkillRecord;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.DbSkillResourceRecord;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillStore;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillVersionConflictException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@link SkillStore} 的 Redis 实现（impl-51 / spec 14 §G）。
 *
 * <p>结构：hash {@code buzhou:skill:<name>} 存主记录字段；set {@code buzhou:skill:index}
 * 存全部名字（findAll 遍历）；hash {@code buzhou:skill:res:<name>} 存资源（field=relativePath，
 * value=JSON）。乐观锁经 WATCH-free 的 Lua-free compute 不可用——用 get-check-set + version 字段，
 * 冲突抛 {@link SkillVersionConflictException}（管理面低并发，CAS 足够）。
 *
 * <p>放置决策与 {@code JdbcSkillStore} 同：feature SPI 实现托管 skills 模块（optional redis 依赖）。
 */
public class RedisSkillStore implements SkillStore {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String KEY_PREFIX = "buzhou:skill:";
    private static final String INDEX_KEY = "buzhou:skill:index";
    private static final String RESOURCE_PREFIX = "buzhou:skill:res:";

    private final StringRedisTemplate redis;
    private final java.util.concurrent.atomic.AtomicLong idSeq =
            new java.util.concurrent.atomic.AtomicLong();

    public RedisSkillStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<DbSkillRecord> findByName(String name) {
        String json = redis.opsForValue().get(KEY_PREFIX + name);
        return json == null ? Optional.empty() : Optional.of(read(json));
    }

    @Override
    public Optional<DbSkillRecord> findPublished(String name) {
        return findByName(name).filter(r -> r.status() == SkillStatus.PUBLISHED);
    }

    @Override
    public List<DbSkillRecord> findAll() {
        Set<String> members = redis.opsForSet().members(INDEX_KEY);
        List<DbSkillRecord> out = new ArrayList<>();
        if (members != null) {
            for (String name : members) {
                findByName(name).ifPresent(out::add);
            }
        }
        out.sort(java.util.Comparator.comparing(DbSkillRecord::name));
        return out;
    }

    @Override
    public DbSkillRecord save(DbSkillRecord record) {
        DbSkillRecord existing = findByName(record.name()).orElse(null);
        if (existing != null && record.version() != existing.version()) {
            throw new SkillVersionConflictException(record.name(), record.version(), existing.version());
        }
        Instant now = Instant.now();
        long id = existing == null ? idSeq.incrementAndGet() : existing.id();
        DbSkillRecord persisted = new DbSkillRecord(id, record.name(), record.description(),
                record.allowedTools(), record.body(), record.status(), record.createdBy(),
                existing == null ? now : existing.createdAt(), now,
                existing == null ? 0 : existing.version() + 1);
        redis.opsForValue().set(KEY_PREFIX + record.name(), write(persisted));
        redis.opsForSet().add(INDEX_KEY, record.name());
        return persisted;
    }

    @Override
    public boolean deleteByName(String name) {
        Boolean removed = redis.delete(KEY_PREFIX + name);
        redis.opsForSet().remove(INDEX_KEY, name);
        deleteResources(name);
        return Boolean.TRUE.equals(removed);
    }

    @Override
    public Optional<DbSkillResourceRecord> findResource(String skillName, String relativePath) {
        Object json = redis.opsForHash().get(RESOURCE_PREFIX + skillName, relativePath);
        return json == null ? Optional.empty() : Optional.of(readResource(String.valueOf(json)));
    }

    @Override
    public List<DbSkillResourceRecord> findResources(String skillName) {
        java.util.Map<Object, Object> entries = redis.opsForHash().entries(RESOURCE_PREFIX + skillName);
        List<DbSkillResourceRecord> out = new ArrayList<>();
        for (Object v : entries.values()) {
            out.add(readResource(String.valueOf(v)));
        }
        out.sort(java.util.Comparator.comparing(DbSkillResourceRecord::relativePath));
        return out;
    }

    @Override
    public DbSkillResourceRecord saveResource(DbSkillResourceRecord record) {
        DbSkillResourceRecord persisted = new DbSkillResourceRecord(
                idSeq.incrementAndGet(), record.skillName(), record.relativePath(),
                record.mediaType(), record.content(),
                record.sizeBytes() >= 0 ? record.sizeBytes()
                        : (record.content() == null ? 0 : record.content().length()),
                Instant.now());
        redis.opsForHash().put(RESOURCE_PREFIX + record.skillName(), record.relativePath(),
                writeResource(persisted));
        return persisted;
    }

    @Override
    public void deleteResources(String skillName) {
        redis.delete(RESOURCE_PREFIX + skillName);
    }

    // ---- JSON 序列化 ----

    private static String write(DbSkillRecord record) {
        try {
            return MAPPER.writeValueAsString(record);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("skill 序列化失败", e);
        }
    }

    private static DbSkillRecord read(String json) {
        try {
            return MAPPER.readValue(json, DbSkillRecord.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("skill 反序列化失败", e);
        }
    }

    private static String writeResource(DbSkillResourceRecord record) {
        try {
            return MAPPER.writeValueAsString(record);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("skill 资源序列化失败", e);
        }
    }

    private static DbSkillResourceRecord readResource(String json) {
        try {
            return MAPPER.readValue(json, DbSkillResourceRecord.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("skill 资源反序列化失败", e);
        }
    }
}
