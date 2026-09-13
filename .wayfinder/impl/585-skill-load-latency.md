# impl 585 — SkillLoadLatency（effort #832）

## 切片

- `buzhou-skills/src/main/java/.../skill/SkillLoadLatency.java` — ConcurrentHashMap<String,Ring>+溢出桶+nearestRank。
- `buzhou-skills/src/test/java/.../skill/SkillLoadLatencyTest.java` — 5 例。

## 口径

- Ring.total/max 累计不随挤出丢失；loads=samples.size()（近窗）。
- 溢出桶与 SkillUsageStats.OVERFLOW 同名（跨面口径一致）。

## 验证

mvn -pl buzhou-skills -am test -Dtest='SkillLoadLatencyTest' → 5/5 绿；快照再生 1 新公共类型。
