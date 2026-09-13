# 795 — fs 沙箱判定计数读面

**What to build:** FileSandbox resolutions/violations 两计数 + 嵌套 SandboxVerdictStats + stats() + 逃逸/空路径/守恒测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] resolutions/violations 计数（violation() 单点）
- [x] SandboxVerdictStats 嵌套 record + stats()
- [x] SandboxVerdictStatsTest（沙箱内/逃逸/空路径/守恒）
- [x] spec 1045 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-core test -Dtest='SandboxVerdictStatsTest'` 全绿 + 既有 FileSandbox 回归绿。commit 见本轮 `feat(core)` 提交。
