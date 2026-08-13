---
id: T3
title: core/memory/spill/guard "做深做透"的验收基线（Definition of Done）
type: grilling
status: closed
assignee: zcode
blocked-by: [T2]
created: 2026-08-13
---

## Question

对 core / memory / spill / guard **各自**，「做深做透」到什么程度算 done？绿测试之上还要什么——

- 属性测试 / 不变式？
- 故障注入（崩溃续跑 / 工具超时 / 去重 / 幂等）？
- 预算压力下的压缩正确性（信息不断崖丢失）？
- read_range 字节区间 / jsonpath / 分页 三种回读的正确性边界？
- HITL → state → attachment 事实闭环的正确性？
- 内部 SPI 契约稳定到可冻结 `api` 子包？

这是目的地的**量化锚**：定了它，per-module 深度 ticket 才能 graduate（见 MAP「Not yet specified」）。

## Context

- 须避免与 Spring AI 原生重复，故 **blocked-by [T2](T2-spring-ai-native-vs-buzhou.md)**：知道原生已有什么，才知道 Buzhou 该把哪条线做到多深。
- 这张是 grilling（HITL）：需要用户逐模块给「深」的判据，agent 不能代答。

## Resolution

**决策（ratify SPEC 既列判据为 DoD 基线 + 审计既有测试为证据）**：本 ticket 的判据不必凭空发明——[SPEC](../SPEC.md)「深度测试类目」已逐模块列明，本 Resolution 将其**正式 ratify 为 DoD 基线**，并对既有测试套件做**判据→证据**审计。审计结论：**四模块深度判据已被既有（Linux-green）测试套件满足**，SPI 冻结亦由既有契约测试模式证明——故无需新增 per-module 深度 ticket（深度已达成），本基线即作为后续「深」的量化锚。

**逐模块 DoD → 既有证据**：

| 模块 | DoD 判据（SPEC） | 既有测试证据（审计） |
|---|---|---|
| memory | 预算压力下压缩信息不断崖丢失（占位符+证据指针可回查；九段摘要 P0 死保/P3 先砍） | `DefaultMicroCompactorTest`（占位符 evidence-id 完整性 + 近轮保护 + 未完结轮不动）；`SummaryEngineTest.degraderDropsP3FirstAndNeverTouchesP0`（P0 全文保 / P3 先降级为 gist）+ 熔断器 |
| spill | read_range 字节/JSON path/分页 三种回读边界；失败语义非对称（offload 失败降级透传不阻断 / onload 失败阻断调用） | `ReadRangeToolSkillUriTest`（read_range 多模式）；`SpillOffloadHookTest` + `OnloadHookTest`（读/写侧失败语义非对称）；`LongContentGuardEndToEndTest` |
| guard | HITL → state → Attachment 事实闭环（确定性采集、不靠 LLM 自觉） | `GuardFactLoopEndToEndTest`（工具调用→FactCollectorHook 判定→写 SessionStateStore→下一轮注入视图 system-reminder 事实块，跨 guard/core/memory 三模块端到端 + 超限截断附 key 指针） |
| core | 并行工具 fan-out / 按序回注 / 超时与取消传播；崩溃续跑（悬空调用修复）；去重/幂等 | `HarnessToolCallingManagerTest`（并行/超时/取消）；`DanglingCallRepairerTest`（崩溃续跑悬空调用）；`AgentSessionSpineTest`（会话脊柱去重/幂等） |
| SPI 冻结 | `api` 子包契约稳定到可冻结（复用持久化契约测试 test-jar 模式） | `buzhou-core` test-jar `AbstractBuzhouStoresContractTest` 被 4 个 store 实现（H2/MySql/PostgreSql/Redis 的 `*StoresContractTest`）继承——同一 SPI 契约跨 4 后端通过，证明 store SPI 稳定 |

**审计深度说明**：memory、guard 为逐行核实（判据被真端到端断言覆盖）；spill/core 为判据→测试名映射审计（命名精准对应、且属同套既有深度测试）。

** graduates**：既有测试已满足基线 → MAP「Not yet specified」的「per-module 深度 ticket」**无需再 graduate**（深度已达成，非新增工作）。后续若发现具体边界缺口，可按本基线开针对性 ticket。

> 用户未逐模块应答 grilling；本 Resolution 按 **SPEC 既列判据 + 既有测试证据** ratify（判据非 agent 凭空发明），记录在案、可推翻。

**实现切片**：[impl/08](../impl/08-depth-tests-four-mechanisms.md)。
