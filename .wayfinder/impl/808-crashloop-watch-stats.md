# 808 — 崩循环探测器类级水位读面

**What to build:** CircuitCrashLoopDetector 静态四计数（opensRecorded/opensTruncated/loopsDetected/recoveriesRecorded）+ 嵌套 CrashLoopWatchStats + stats()/resetForTest() + 截断/成环/恢复/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [ ] 四计数落点（入表 OPEN/封顶丢弃/循环闩锁/恢复清除）
- [ ] CrashLoopWatchStats 嵌套 record + stats() + resetForTest()
- [ ] CrashLoopWatchStatsTest（记录/截断/成环/恢复再成环/null 不入账/reset 六测）
- [ ] spec 1056 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-resilience -am test -Dtest='CrashLoopWatchStatsTest'` 全绿 + 既有 CircuitCrashLoopDetector 回归绿。commit 见本轮 `feat(resilience)` 提交。
