# 910 — 分支缺口批次 1：observe-otel（R8）

**What to build:** OtelBridgeSink 分支补测（重复开启/驱逐护栏/spanName 回退/sessionTrace 回退与上界/类型适配器/空 payload/故障隔离）+ OtelProperties 默认与 fail-fast 分支。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] OtelBridgeSinkBranchTest（同包，InMemorySpanExporter hermetic + Proxy Tracer 故障注入）
- [x] OtelPropertiesTest（config 包，构造分支矩阵）
- [x] spec 1207 + README 行
- [x] 验证：32 用例全绿；OtelBridgeSink 87% / OtelProperties 100%；显形 T1824 单列修复

## Done

验证：observe-otel 32 用例全绿（新增 16）；分支提升如上；T1824 一行修复独立 commit。commit 见本轮 `fix(otel)` 与 `test(otel)` 提交。
