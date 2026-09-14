# 817 — 完成轮检测器读面

**What to build:** DefaultCompletedTurnDetector 静态三计数（detectCalls/spansDetected/toolCallTurnsSeen）+ 嵌套 CompletedTurnStats + stats()/resetForTest() + 检出/失能信号/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数落点（入口/检出累计/分母消息数）
- [x] CompletedTurnStats 嵌套 record + stats() + resetForTest()
- [x] CompletedTurnStatsTest（检出/失能信号/归零三测）
- [x] spec 1065 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-memory -am test -Dtest='CompletedTurnStatsTest'` 全绿 + 既有 CompletedTurnDetector 回归绿。commit 见本轮 `feat(memory)` 提交。
