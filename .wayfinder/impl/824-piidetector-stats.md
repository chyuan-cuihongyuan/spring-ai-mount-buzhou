# 824 — PII 检测引擎读面

**What to build:** PiiDetector 静态四计数（scanCalls/scansWithHits/matchesFound/pseudonymizeCalls）+ 嵌套 PiiDetectorStats + stats()/resetForTest() + 命中/干净/假名化/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四计数落点（scan 入口/命中非空/匹配累计/假名化入口）
- [x] PiiDetectorStats 嵌套 record + stats() + resetForTest()
- [x] PiiDetectorStatsTest（命中/干净/假名化/归零四测）
- [x] spec 1072 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard -am test -Dtest='PiiDetectorStatsTest'` 全绿 + 既有 PiiDetector 回归绿。commit 见本轮 `feat(guard)` 提交。
