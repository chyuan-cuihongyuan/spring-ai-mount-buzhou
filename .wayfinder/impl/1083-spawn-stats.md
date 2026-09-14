# 1083 — 会话 spawn 统计读面

**What to build:** SessionSpawnStats 公共静态面（attempts/successes/collisions/steals+activePeak）+ doSpawn 四点埋点 + 三测。

**Blocked by:** None.

**Status:** done

- [x] SessionSpawnStats（core/session 公共静态面，record 方法 public——R14 同款教训）
- [x] DefaultAgentRuntime.doSpawn 四点埋点
- [x] SessionSpawnStatsTest 三测
- [x] spec 1432 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='SessionSpawnStatsTest'` 3/3 绿。
