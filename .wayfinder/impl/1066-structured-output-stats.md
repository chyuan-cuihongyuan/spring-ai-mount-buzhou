# 1066 — 结构化输出 REASK 读数

**What to build:** StructuredOutputStats 公共静态面（漏斗五计数+双守恒+firstPassRate）+ chatForEntity 五点埋点 + 四测。

**Blocked by:** None.

**Status:** done

- [x] StructuredOutputStats（core/session；record 方法 public——初版包私有跨包不可见编译错评审修正）
- [x] DefaultAgentSession.chatForEntity 五点埋点（行为逐位不变）
- [x] StructuredOutputStatsTest 四测（E2E 三流+哨兵）
- [x] spec 1413 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='StructuredOutputStatsTest,StructuredOutputEndToEndTest'` 8/8 绿。
