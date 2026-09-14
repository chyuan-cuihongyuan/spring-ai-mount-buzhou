# 818 — Deno 沙箱探测读面

**What to build:** DenoSandbox 静态五计数（availableCalls/probeCacheHits/probes/probeSuccesses/probeUnavailables）双守恒 + 嵌套 DenoProbeStats + stats()/resetForTest() + 命中/重探/成败/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 五计数落点（入口/缓存短路径/重探测/成败两桶）
- [x] DenoProbeStats 嵌套 record + stats() + resetForTest()
- [x] DenoProbeStatsTest（成功探测/TTL 命中/异常不可用/双守恒/reset 五测）
- [x] spec 1066 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard -am test -Dtest='DenoProbeStatsTest'` 全绿 + 既有 DenoSandbox 回归绿。commit 见本轮 `feat(guard)` 提交。
