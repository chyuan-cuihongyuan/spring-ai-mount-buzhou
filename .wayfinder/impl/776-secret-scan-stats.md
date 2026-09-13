# 776 — 密钥扫描计数读面

**What to build:** SecretScanner scanCalls/findings/redactions 三计数 + 嵌套 SecretScanStats + stats() + AWS 示例键确定性测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数埋点（scan 入口/命中按条/redact 实际替换）
- [x] SecretScanStats 嵌套 record + stats()
- [x] SecretScanStatsTest（调用/命中/三齐动/幂等早返不计/实例隔离）
- [x] spec 1023 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard test -Dtest='SecretScanStatsTest,SecretScannerEntropyTest'` 全绿。commit 见本轮 `feat(guard)` 提交。
