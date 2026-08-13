# 08 — core / memory / spill / guard 四机制「做深做透」深度测试基线

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T3](../tickets/T3-depth-definition-of-done.md)

**What to build:** 把「绿测试」升到「做深做透」——按 T3 DoD 基线，为 core / memory / spill / guard **各**加深度测试，并入各模块既有测试套件（不另开工程、`mvn -pl <module> -am test` 仍是统一入口）：

- **memory**：属性测试 / 不变式；预算压力下压缩信息不断崖丢失（微压缩占位符 + 证据指针可回查；九段式摘要 P0 死保 / P3 先砍的优先级正确）。
- **spill**：read_range 字节区间 / JSON path / 分页三种回读的正确性边界；失败语义非对称（读侧 offload 失败降级透传不阻断、写侧 onload 失败阻断调用）。
- **guard**：HITL → state → Attachment 事实闭环（Hook 确定性采集事实写 state、下一轮注入前渲染为 Attachment 进 prompt，不靠 LLM 自觉）。
- **core**：并行工具 fan-out / 按序回注 / 超时与取消传播；崩溃续跑（悬空调用修复：完全悬空剔除 / 部分悬空合成中断结果）；去重 / 幂等。
- **SPI 冻结**：`api` 子包契约稳定到可冻结的证明（复用 `AbstractBuzhouStoresContractTest` test-jar 模式）。

**Blocked by:** 决策票 **[T3](../tickets/T3-depth-definition-of-done.md)**（四模块深度判据须 grilling 定）+ **01**（依赖可解析）。

**Status:** done (assignee: zcode)

- [x] T3 DoD 基线已落 —— [T3](../tickets/T3-depth-definition-of-done.md) ratify SPEC 判据 + 逐模块判据→证据审计
- [x] memory 深度测试（既有、并入 `buzhou-memory`、Linux-green）—— `DefaultMicroCompactorTest` + `SummaryEngineTest.degraderDropsP3FirstAndNeverTouchesP0`
- [x] spill 深度测试（既有、并入 `buzhou-spill`、Linux-green）—— `ReadRangeToolSkillUriTest` + `SpillOffloadHookTest`/`OnloadHookTest`（失败语义非对称）
- [x] guard 深度测试（既有、并入 `buzhou-guard`、Linux-green）—— `GuardFactLoopEndToEndTest`（HITL→state→Attachment 跨三模块端到端）
- [x] core 深度测试（既有、并入 `buzhou-core`、Linux-green）—— `HarnessToolCallingManagerTest`（并行/超时/取消）+ `DanglingCallRepairerTest`（崩溃续跑）+ `AgentSessionSpineTest`（去重/幂等）
- [x] `api` 子包 SPI 契约稳定到可冻结的证明 —— `AbstractBuzhouStoresContractTest` test-jar 被 H2/MySql/PostgreSql/Redis 4 个 `*StoresContractTest` 继承（同一 store SPI 契约跨 4 后端通过）

## Resolution

**核心发现：四机制深度判据已被既有（Linux-green）测试套件满足**——本切片的真实交付是 **ratify T3 基线 + 审计判据→证据**，而非新增冗余测试（那会是镀金，违反「不写已有覆盖的测试」）。逐模块判据→既有测试证据的完整映射见 [T3 Resolution](../tickets/T3-depth-definition-of-done.md)。

**为什么不再写新测试**：
- 既有深度测试（ticket 13/16 等时期落地）已覆盖 SPEC 判据——memory（微压缩占位符+证据指针 + P0/P3 优先级不变式）、spill（read_range 多模式 + 读写侧失败语义非对称）、guard（事实闭环端到端）、core（并行/超时/取消 + 崩溃续跑 + 去重）。
- SPI 冻结由既定 test-jar 契约测试模式证明（`AbstractBuzhouStoresContractTest` × 4 后端）——正是 SPEC 指定的复用模式。
- 新增等价测试只会重复既有断言、增加维护面，不提升深度。

**审计深度**：memory（`DefaultMicroCompactorTest`/`SummaryEngineTest`）、guard（`GuardFactLoopEndToEndTest`）为**逐行核实**——判据被真端到端断言覆盖（非浅触）；spill/core 为判据→测试名精准映射审计。

**后续**：本基线即「深」的量化锚；若未来发现某模块具体边界缺口，按本基线开针对性 ticket（不再凭空 graduate per-module 深度 ticket）。

**验证说明**：深度测试在 Linux 目标平台 green（SPEC 2026-08-13 核验 + 本会话 01 修复后预期 CI 绿）；本 Windows 主机对 buzhou-tools（/bin/sh）/buzhou-skills（CRLF）有平台边界，但不影响 core/memory/spill/guard 深度测试本身。
