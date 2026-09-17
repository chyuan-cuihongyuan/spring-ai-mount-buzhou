# 876 — PII 出站脱敏读面

**What to build:** PiiEventRedactor 静态四计数（eventsProcessed/redacted/cleanPassthrough/failOpen）+ 嵌套 PiiEventRedStats + stats()/resetForTest() + 三结局/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四计数落点（入口/改写/无命中/异常透传）
- [x] PiiEventRedStats 嵌套 record + stats() + resetForTest()
- [x] PiiEventRedStatsTest（改写/透传/守恒/归零四测）
- [x] spec 1211 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard -am test -Dtest='PiiEventRedStatsTest'` 全绿。commit 见本轮 `feat(guard)` 提交。
