package io.github.chyuan_cuihongyuan.buzhou.skill.store;

import io.github.chyuan_cuihongyuan.buzhou.skill.SkillStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link SkillStore} 的内存默认实现（与持久化五 SPI 的内存默认实现同模式）。
 *
 * <p>线程安全；{@link #save} 按 name upsert，version 递增；资源按 (skillName, relativePath) 唯一。
 */
public class InMemorySkillStore implements SkillStore {

    private final ConcurrentHashMap<String, DbSkillRecord> skills = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DbSkillResourceRecord> resources = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong();

    @Override
    public Optional<DbSkillRecord> findByName(String name) {
        return Optional.ofNullable(skills.get(name));
    }

    @Override
    public Optional<DbSkillRecord> findPublished(String name) {
        return findByName(name).filter(r -> r.status() == SkillStatus.PUBLISHED);
    }

    @Override
    public List<DbSkillRecord> findAll() {
        return skills.values().stream()
                .sorted(Comparator.comparing(DbSkillRecord::name))
                .toList();
    }

    @Override
    public DbSkillRecord save(DbSkillRecord record) {
        Instant now = Instant.now();
        // 乐观锁（spec 04：version 为管理 API 并发编辑兜底）：更新时携带的 version 须等于
        // 库内现值（新建传 0 且库内不存在），冲突即抛，不静默覆盖
        return skills.compute(record.name(), (name, existing) -> {
            if (existing != null && record.version() != existing.version()) {
                throw new SkillVersionConflictException(name, record.version(), existing.version());
            }
            long id = existing == null ? idSeq.incrementAndGet() : existing.id();
            Instant created = existing == null ? now : existing.createdAt();
            int version = existing == null ? 0 : existing.version() + 1;
            return new DbSkillRecord(id, record.name(), record.description(), record.allowedTools(),
                    record.body(), record.status(), record.createdBy(), created, now, version);
        });
    }

    @Override
    public boolean deleteByName(String name) {
        DbSkillRecord removed = skills.remove(name);
        if (removed != null) {
            deleteResources(name);
            return true;
        }
        return false;
    }

    @Override
    public Optional<DbSkillResourceRecord> findResource(String skillName, String relativePath) {
        return Optional.ofNullable(resources.get(resourceKey(skillName, relativePath)));
    }

    @Override
    public List<DbSkillResourceRecord> findResources(String skillName) {
        return resources.values().stream()
                .filter(r -> r.skillName().equals(skillName))
                .sorted(Comparator.comparing(DbSkillResourceRecord::relativePath))
                .toList();
    }

    @Override
    public DbSkillResourceRecord saveResource(DbSkillResourceRecord record) {
        long size = record.sizeBytes() >= 0 ? record.sizeBytes()
                : (record.content() == null ? 0 : record.content().length());
        DbSkillResourceRecord persisted = new DbSkillResourceRecord(
                idSeq.incrementAndGet(), record.skillName(), record.relativePath(),
                record.mediaType(), record.content(), size, Instant.now());
        resources.put(resourceKey(record.skillName(), record.relativePath()), persisted);
        return persisted;
    }

    @Override
    public void deleteResources(String skillName) {
        resources.entrySet().removeIf(e -> e.getValue().skillName().equals(skillName));
    }

    private static String resourceKey(String skillName, String relativePath) {
        return skillName + "::" + relativePath;
    }
}
