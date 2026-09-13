# 793 — time-travel fork 操作计数读面

**What to build:** SessionForks 嵌套 ForkStats（forksCreated/messagesCopied）+ stats() + forkFrom 计数埋点 + 计数测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ForkStats 嵌套 record
- [x] forksCreated/messagesCopied 计数埋点
- [x] stats() 只读快照
- [x] ForkStatsTest（计数/累计/原会话不动回归）
- [x] spec 1041 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-memory test -Dtest='ForkStatsTest'` 全绿。commit 见本轮 `feat(memory)` 提交。
