# Spec: 崩溃中轮次恢复 + 幂等（M1 — 恢复语义分档 + 持久化强度三档 + 幂等三件套）

Status: ready-for-agent
源决策: wayfinder「production-readiness」05 号票（grilling 收口，决策=做）+ 02 号票（底座留白盘点）
里程碑: M1 稳定基线（路线图「03 M1 先行 → 05 依赖恢复语义与 SPI 现状梳理」）

---

## Problem Statement

进程崩溃时，**正在执行中的轮次（in-flight turn，含在途模型调用与在途工具调用）**目前是**半保护、口径不明**的：

- **恢复语义不显式、不可选**：现状是「加载历史时跑 `DanglingCallRepairer` 修复悬空调用 → 等用户重新驱动」，但这条路径是隐式的、未文档化、无事件留痕。无人值守/长任务会话想要「崩溃后自动续跑」没有 opt-in 出口；交互式会话想确保「绝不擅自重跑」也没有显式契约。
- **持久化强度不可控**：会话状态落盘走 `BuzhouChatMemory.add() → messageStore.append()`，没有任何 durability 表达——一次崩溃究竟丢多少（只丢在途工具结果？丢整轮？丢到上一次完结轮次？）完全取决于存储实现，运维无法在「一致性 vs 吞吐」之间显式取舍。
- **副作用工具无幂等保护**：`DanglingCallRepairer.tryReplay()` 对幂等工具在修复时**重新执行**（at-least-once）；对非幂等副作用工具，崩溃发生在「工具已执行、结果未落库」窗口时会**重复执行**（重复扣款/重复下单），框架层没有去重闸门。工具调用本质是 at-least-once 投递，缺一道「去重 = 效果恰好一次」的兜底。
- **崩溃检测本身不可靠**：`DefaultAgentRuntime` 取得 90s 租约后**从不续约**（`SessionLeaseStore.renew()` 在主代码无调用方）——长轮次会被误判为崩溃、被另一实例 steal，破坏单活跃实例不变量。

从用户视角：业务 Agent 作者只想让崩溃后的会话**确定性地**回来——要么干净地等重驱动、要么按约定自动续跑——且不重复执行副作用工具；平台集成者想按部署显式选择「多强的一致性、多大的吞吐代价」；SRE 想在 observability 里看清「这一轮崩溃后被怎么处理了、哪个工具走了去重」。

## Solution

不周山在 core 内补全**崩溃恢复 + 幂等**三支柱 + 一项基础，**不新增模块、不新增 SPI**（与 05 决策一致），复用既有的「租约 + 消息即检查点 + 悬空修复」底座：

- **恢复语义分档**：默认「轮次作废」（现状语义明确化 + 文档化 + 事件化）；opt-in「自动重驱动续跑」（加载 + 悬空修复后，对被中断轮次重新发起模型调用，无需用户输入）。自动重驱动的崩溃循环风险由 03/04 韧性层熔断配合兜底。
- **持久化强度三档**（直接对标 LangGraph durability）：`sync`（下一步前同步落盘）/ `async`（默认，边执行边落盘）/ `exit`（仅退出时落盘，最高吞吐）；经既有 `UnitOfWork` + 存储写路径表达，按绑定级配置；`exit` 档崩溃丢整轮的风险由恢复语义兜底。
- **幂等三件套**（业界空白，自主推演）：① 工具声明幂等性（扩既有 `@BuzhouTool.idempotent`，副作用工具默认非幂等）② 幂等键——框架默认生成（会话+轮次+调用序号），业务可覆盖（如订单号）③ 去重记录——重试/重放命中键时返回首次结果而非重执行（at-least-once 调用 + 去重 = 效果恰好一次），复用既有 per-session 存储 SPI 扩展原子语义。
- **基础项**：租约心跳（轮次执行期 `renew()`），让「租约过期=崩溃」的检测信号在长轮次上仍然成立。

恢复路径完整闭环：**租约过期/被 steal → 新实例 spawn 获取租约 → 加载历史 → 悬空修复（既有）→ 按持久化强度档位决定可恢复面 → 按恢复语义档位决定作废/续跑 →（续跑时）幂等去重保证副作用不重**。事件全程进既有 observability 通道。

## User Stories

1. 作为 Agent 应用开发者，我希望进程崩溃后 in-flight 轮次**确定性地**恢复（默认：作废 + 修复 + 等我下一次输入），这样我不丢失整个会话、且行为可预期。
2. 作为 Agent 应用开发者，我希望默认恢复语义被**文档化并以事件留痕**，这样我能从 observability 还原「中断轮次被怎么处理了」。
3. 作为 Agent 应用开发者（无人值守/长任务会话），我希望 **opt-in 自动重驱动**，这样崩溃后会话无需新用户输入即继续被中断的轮次。
4. 作为 Agent 应用开发者，我希望自动重驱动**默认关闭**，这样交互式会话不会在我不知情下重跑轮次。
5. 作为 SRE，我希望自动重驱动的**崩溃循环被兜底**（复用 03/04 韧性层熔断），这样反复崩溃的轮次不会无限重试。
6. 作为 Agent 应用开发者，我希望恢复**复用既有的悬空修复**（`DanglingCallRepairer`），这样中断的工具调用在重驱动前先被修复。
7. 作为 Agent 应用开发者，我希望恢复由**租约交接**（过期/steal）在新 spawn 上触发，这样机制走既有的「同会话单活跃实例」原语、不另起一套。
8. 作为平台集成者，我希望按部署/绑定**选择持久化强度档位**（sync/async/exit），这样我能在一致性与吞吐之间显式取舍。
9. 作为平台集成者，我希望 **async 为默认档位**，这样典型部署得到良好吞吐 + 最终持久。
10. 作为 Agent 应用开发者，我希望 **sync 档保证「下一步前当步写入已持久」**，这样相邻步骤间的崩溃至多丢在途那一步。
11. 作为 Agent 应用开发者，我知晓 **exit 档仅在会话退出时落盘、崩溃可能丢整轮**，这样我只在高吞吐、可丢轮的工作负载上选它。
12. 作为 SRE，我希望**生效的持久化档位进 observability**，这样我能审计每个会话的一致性契约。
13. 作为 Harness 集成者，我希望持久化档位**不新增 SPI**（经既有 `UnitOfWork` + 存储写路径表达），这样 SPI 面保持稳定。
14. 作为存储实现者（jdbc/redis），我希望档位语义被**共享契约测试锁定**，这样所有后端对 sync/async/exit 的行为一致。
15. 作为工具作者，我希望**声明工具是否幂等**（副作用工具默认非幂等），这样框架知道重执行是否安全。
16. 作为 Agent 应用开发者，我希望框架**自动生成幂等键**（会话+轮次+调用序号），这样去重无需逐工具写代码。
17. 作为业务工具开发者，我希望**用业务标识覆盖幂等键**（如从入参取订单号），这样语义幂等的业务工具按正确的键去重。
18. 作为 Agent 应用开发者，我希望副作用工具调用在崩溃 + 恢复后**效果上只执行一次**（去重返回首次结果、不重执行），这样崩溃在途不会重复扣款。
19. 作为 Agent 应用开发者，我希望去重在**重试（瞬时故障）与恢复重放**两条路径上都生效，这样两条路径都被保护。
20. 作为 SRE，我希望**去重命中进事件流**（键 + 工具），这样我能看到哪些工具走了去重记录而非真实执行。
21. 作为 Harness 集成者，我希望去重记录**复用既有 per-session 存储 SPI**（扩展原子语义），这样不新增持久化 SPI。
22. 作为 Agent 应用开发者，我知晓**去重作用域 = 会话内**，这样我清楚跨会话的重复防护不在框架职责内。
23. 作为 SRE，我希望**长轮次执行期租约被续约（心跳）**，这样活的长轮次不被误判为崩溃。
24. 作为 Agent 应用开发者，我希望**崩溃检测（租约过期）可靠**，这样恢复只在真崩溃时触发。
25. 作为平台集成者，我希望恢复/持久化/幂等参数**经 yml 配置**，这样调参无需改代码。
26. 作为 Agent 应用开发者，我希望机制 **safe-by-default**（默认 async 档、作废恢复、去重开），这样引入即正确、无需额外配置。
27. 作为 Agent 应用开发者，我希望有**一键关闭自动重驱动**（及恢复增强）的开关，这样排障时可回退基线行为。
28. 作为 Harness 集成者，我希望本机制作为 **core 扩展**（不新增模块），因为恢复/幂等横跨会话生命周期 + 执行脊柱 + 写路径。
29. 作为 Harness 集成者，我希望**星形依赖不被破坏**（不新增 feature 模块耦合），这样 16 模块物理无环图不变。
30. 作为平台集成者，我希望 **M1 落地默认 + yml 两层**（绑定级覆盖待 policy 消费管线打通再纳入），与同级机制（03 等）处理一致。
31. 作为平台架构者，我希望**去重记录与 09 工具结果缓存分离**（去重=正确性防重放、缓存=效率省调用），这样两关注点不纠缠。
32. 作为平台架构者，我希望 **exit 档 flush 与 06 优雅停机 drain 联动**（受控停机必须 flush 缓冲写入），这样停机不丢缓冲数据。
33. 作为 Agent 应用开发者，我希望恢复事件走**既有 observability 通道**（不新增 SPI），这样 SRE 在一处看全。
34. 作为 Agent 应用开发者，我希望公共 API 变更（工具幂等声明扩展、键提取器、恢复选项）**全部 additive / default 兼容**，这样既有工具与集成不受破坏。
35. 作为平台架构者，我希望自动重驱动的崩溃循环兜底**复用 03/04 韧性层熔断**，这样不另造循环保护。

## Implementation Decisions

### 改动面与定位

- **core 扩展，不新增模块**（与 05 决策「core 扩展（session/exec）+ store 契约」一致）。恢复/幂等横跨三处：会话生命周期（`DefaultAgentSession` / `DefaultAgentRuntime`）、执行脊柱工具调用（`HarnessToolCallingManager`）、记忆写路径（`BuzhouChatMemory` / `UnitOfWork` / `MessageStore`）；这些都在 core 内。store 实现（jdbc/redis）按契约扩展档位语义。
- **不新增 SPI**（05 决策）：恢复走既有租约 + `MessageStore`（消息即检查点）+ `DanglingCallRepairer`；去重记录复用既有 per-session 存储 SPI 并**扩展其原子语义**（细则见下「幂等去重」）；持久化档位经既有 `UnitOfWork` + 存储写路径表达。
- **公共 API 变更（全部 additive，PR 须说明兼容性）**：`@BuzhouTool` 的幂等声明从「仅原子工具」扩到全部工具（既有 `idempotent` 元素保留）；新增**可选**键提取器声明（default no-op → 走框架默认键）；`SpawnOptions` 新增**可选**恢复策略覆盖（default 走配置）。既有工具与集成源码/二进制兼容。

### 恢复语义分档

- **触发点**：租约交接——持有实例崩溃 → 租约过期（TTL 到点）或被新实例 `steal` → 新 `spawn()` 获取租约 → 加载历史 → 悬空修复（既有 `DanglingCallRepairer`，已在 `BuzhouChatMemory.get()` 每次加载时运行）→ 按**恢复策略档位**决定后续动作。
- **两档（决策表）**：

  | 档位 | 触发条件 | 行为 | 默认 |
  |---|---|---|---|
  | 轮次作废（VOID） | 总是可选 | 修复历史后**等用户下一次输入**（现状语义，明确化 + 事件化） | **是** |
  | 自动重驱动（AUTO_RESUME） | opt-in（绑定级/yml） | 修复历史后，若历史结尾为「被中断轮次」，**无需用户输入**重新发起模型调用续跑该轮 | 否 |

- **「被中断轮次」判定（决策）**：续跑 iff 加载+修复后的历史**结尾没有终结性助手回复**——即最后一轮只到「助手发了工具调用 / 工具结果被悬空修复补齐」而**没有最终的、不含工具调用的助手消息**。`DanglingCallRepairer` 已能标注中断态（`INTERRUPTED_RESULT` / `RepairEvent.action`），续跑据此信号触发；完结轮次（已有终结回复）不续跑。
- **崩溃循环兜底**：AUTO_RESUME 反复崩溃重驱动时，由 03/04 韧性层熔断掐断（不在本机制重造）；M1 在熔断未就绪前先记事件 + 设硬顶次数（保守）。
- **基础项：租约心跳（决策，小改）**：轮次执行期对 `SessionLeaseStore.renew()` 发心跳（间隔 < TTL，复用会话既有虚拟线程执行器），让长轮次不被误判崩溃。**这是恢复信号可靠性的前提**；与 06 优雅停机共用「会话生命周期」关注点，实现期与 06 对齐心跳间隔 vs TTL。

### 持久化强度三档

- **三档（对标 LangGraph durability）**：`SYNC` / `ASYNC`（默认）/ `EXIT`，绑定级配置（M1 地板：默认 + yml；绑定级覆盖待 policy 消费管线打通）。
- **表达方式（决策，不新增 SPI）**：档位是**存储实现侧的写缓冲策略**，编排方（`BuzhouChatMemory` 写路径）**不按档位分支**——
  - `SYNC`：`append` / `put` 同步落盘后返回（相邻步骤间崩溃至多丢在途那一步）；
  - `ASYNC`：`append` / `put` 交给写缓冲、shortly after 持久（默认，吞吐优先）；
  - `EXIT`：`append` / `put` 仅入缓冲，**会话关闭时 flush**（最高吞吐，崩溃丢整轮，由恢复语义兜底）。
- 档位语义由**共享契约测试锁定**（见 Testing Decisions），所有后端一致。
- **`EXIT` 档与 06 联动**：受控停机（06 drain）必须触发 `EXIT` 档 flush；本机制提供 flush 钩子，06 的 SmartLifecycle 停机步骤调用之。

### 幂等三件套

- **① 声明（既有扩展）**：`@BuzhouTool.idempotent` 已存在（驱动 `DanglingCallRepairer` 重放资格）；扩到全部工具，**副作用工具默认非幂等**。装配方既已把 `idempotentToolNames` 汇入 `RuntimeConfig`，沿用该通道。
- **② 幂等键（决策）**：
  - 框架默认键 = `{sessionId, turnSeq, toolCallId}`（`BuzhouMessage` 已具 `turnSeq`；`toolCallId` 为每次调用稳定 id，天然满足「会话+轮次+调用序号」）；
  - 业务覆盖 = 工具声明**键提取器**（从工具入参取业务标识，如订单号）；提取器为 default no-op，未声明则走默认键。
- **③ 去重记录（决策，复用既有 SPI 扩展原子语义）**：
  - **写入时机（关键）**：执行脊柱在**工具调用前后**包一层——调用前以幂等键**原子 reserve**（put-if-absent）一条 pending 记录，调用成功后**回填结果**；这样「工具已执行、消息未 append」的崩溃窗口里，去重记录已捕获结果。
  - **去重命中**：重试（瞬时故障重试）与恢复重放（`DanglingCallRepairer`）两条路径，在执行/重放前**先查键**——命中已回填结果则**直接返回首次结果、不重执行**；命中 pending（前次未完成）则按既定取消/续跑策略处理。
  - **与 `DanglingCallRepairer` 的整合（决策）**：现有 `tryReplay()` 对幂等工具**重执行**（at-least-once）；改为**先查去重记录**——命中则用存储结果**合成工具响应**（不重执行），未命中才决定（幂等→可重执行 / 非幂等→合成交断结果）。at-least-once 调用 + 去重 = **效果恰好一次**。
  - **存储位置**：复用既有 per-session 存储（`SessionStateStore` 形态，per-session KV + 已有 CAS 语义 `deleteIfValueMatches`）；去重所需**原子 put-if-absent / reserve-then-fill** 作为契约语义补充（jdbc 行级锁/影响行数判定、redis Lua、内存 CAS）——此即 05 决策「去重记录复用现有 SPI 扩展」的细则。
- **作用域（决策）**：会话内。跨会话去重不归框架（键命名空间 = sessionId）。

### 事件进 observability

- 新增普通观测事件类型（**非**治理/审计族——那是 M2/M3）：`turn-recovered`（带 action: voided / auto-resumed）、`dedup-hit`（带键 + 工具名）、`durability-tier`（会话打开时记录生效档位）、`resume-skipped-crashloop`（崩溃循环兜底触发）。
- 悬空修复事件复用既有 `DanglingCallRepairer.RepairEvent`。走会话既有事件通道，**不新增 SPI**。

### 配置范围（M1 地板）

- `@ConfigurationProperties(prefix="buzhou.recovery")`（或并入既有命名空间，record + boxed 类型、null=未配置，对齐 `SpillProperties` 模板）：`enabled` / `resume-strategy`（VOID 默认）/ `durability-tier`（ASYNC 默认）/ `crashloop-hard-cap`（M1 兜底次数）/ 幂等默认开关。
- **绑定级层移出 M1**：与 03 同口径——`LayeredPolicy` 脚手架未连装配、`BindingPolicy.mechanismOverrides` 仅在测试中断言；M1 不半接这根线，绑定级覆盖作为 policy 消费管线打通后的前置项再纳入。

### 工程纪律

- 不新增模块、不新增 SPI（05 决策）；行为变更带测试（CONTRIBUTING 约定）；store 语义扩展（档位 + 去重原子性）走**契约测试模式**（core 发布 test-jar，jdbc/redis 继承）。
- 公共 API 变更（`@BuzhouTool` 幂等声明扩展、键提取器、`SpawnOptions` 恢复策略）在 PR 描述说明兼容性影响（全 additive / default）。
- 借鉴 LangGraph Durability 三档语义、Checkpointer SPI + 官方契约测试包（与不周山五 SPI + test-jar 模式同向）、`put_writes`「已完成写入不重放」思想（去重蓝本）。

## Testing Decisions

### 什么是好测试

只测**外部行为**——最终回复 + observability 事件流 + 工具调用计数器（用于证明「恰好一次」），不测恢复循环 / 去重表的内部状态与私有字段。恢复「作废还是续跑」「工具是否重执行」「哪一档持久化在崩溃后留下什么」一律从外部观察判定——这是仓库既有的 e2e 测试哲学（同 `HookEndToEndTest` / `AgentSessionSpineTest`）。

### 缝合点（已与 owner 确认：3 个，全部复用既有缝合点，不新增）

1. **主缝合点（最高、复用既有）——端到端经 `AgentRuntime.spawn().chat()` + 可注入故障的 ChatModel/工具**：
   - 复用 core test-jar 的 `ScriptedChatModel`（model-resilience 已扩「抛错」能力，本机制再扩「中途崩溃」语义）+ 一个**带调用计数器的副作用工具**（可脚本化「第 1 次调用后进程崩溃」）。
   - 经真实 `Buzhou.runtime(...)` 装配出完整链路后：跑一轮 → 用**租约交接**模拟崩溃（让租约过期 / `SpawnOptions.withSteal()` 起 второй实例，同 `sessionId`）→ 断言**最终回复 + 事件流 + 工具调用计数器**。
   - 覆盖：① 默认作废（历史被修复、等用户、不自动续跑）；② opt-in 自动重驱动（无需用户输入续跑被中断轮次）；③ 崩溃循环兜底（硬顶次数 / 熔断就绪后交接）；④ 幂等副作用工具**恰好一次**（计数器 == 1，去重命中返回首次结果）；⑤ 持久化档位决定可恢复面（sync 丢至多在途一步、exit 可能丢整轮）。
   - **确定性**：用 `CountDownLatch` / 可控故障注入模拟「崩溃窗口」（对齐 `HarnessToolCallingManagerTest` 的超时/cancel 手法，**不用** wall-clock sleep）；租约过期用**可调 TTL** 的测试 `SessionLeaseStore` 注入短 TTL，不真等 90s。
   - 先验：`HookEndToEndTest`、`AgentSessionSpineTest`、`DanglingCallRepairerTest`（同一 e2e / 修复形态）、`HarnessToolCallingManagerTest`（虚拟线程 + cancel + 故障注入手法）。

2. **次缝合点（复用既有 store 契约）——扩展 `AbstractBuzhouStoresContractTest`**：
   - 在共享契约里新增**持久化档位断言**（SYNC：append 后立即可见且抗崩溃 / ASYNC：shortly after 持久 / EXIT：仅 close 后持久）与**去重记录原子语义断言**（put-if-absent / reserve-then-fill / 命中返回首次结果）。
   - jdbc/redis/内存三后端继承同一契约 → 锁定跨后端一致性；**不新增缝合点**，只扩既有契约。
   - 先验：`AbstractBuzhouStoresContractTest` + `InMemoryStoresContractTest` / `RedisStoresContractTest`（既有「穷举 SPI 表面」模式）。

3. **支持缝合点（复用既有纯函数表测试形态）——扩展 `DanglingCallRepairerTest`**：
   - 把**幂等键派生**（默认键 = 会话+轮次+调用序号；业务覆盖）与**去重命中/未命中决策表**做成纯函数穷举枚举（每个 provider/工具形态 → 期望键 / 期望重执行 vs 合成）。
   - 纯函数、无链路、廉价，覆盖比经 e2e 枚举更清晰；形态类比既有 `DanglingCallRepairerTest`。
   - 先验：`DanglingCallRepairerTest`（既有修复决策表测试）。

### 被测模块

- `buzhou-core`（主：session / exec / memory 写路径 / `UnitOfWork` / 租约心跳）+ store 实现（jdbc/redis，经契约测试）。
- 不涉及新模块；store 语义扩展走契约测试模式。

## Out of Scope

- **跨会话幂等**（键作用域 = 会话内；跨会话/全局去重不归框架）。
- **工具内部业务逻辑正确性**（框架只保证「效果上不重复执行」，不保证工具自身正确）。
- **绑定级 policy 消费**：M1 只到默认 + yml；绑定级覆盖待 policy 消费管线打通。
- **06 优雅停机的完整 drain 协议**（拒新/等轮次/超时强杀）：归 06；本机制只提供 `EXIT` 档 flush 钩子供 06 调用。
- **多实例/集群级一致性协议**（如 Quorum/共识）：本机制只管单实例租约交接；分布式一致性归部署层。
- **09 工具结果缓存**（效率省调用）：与去重（正确性防重放）分工，归 09。
- **03/04 韧性层熔断与降级链的完整设计**：本机制只**复用**其熔断做崩溃循环兜底，不重新设计。
- **内容拒绝/内容安全的治理策略**（归 12）。
- **成本治理**（11）、**死循环与失控检测**（08）——不同家族。
- **流式已发 token 的中途恢复重驱动**（边界同 03：流式已发 token 不中途重试/续发；流式崩溃按作废语义处理）。

## Further Notes

- **管辖 ADR**：wayfinder「production-readiness」05 号票（决策=做、恢复语义分档 + 持久化三档 + 幂等三件套、不新增 SPI）+ 02 号票（底座无 checkpoint/durability 控制、无幂等——不周山合法空间）。本 Spec 落地 05 的 **M1** 子集。
- **借鉴清单**（一手链接见 `docs/production-readiness/references.md` 的 05 条目）：
  - LangGraph Durability 三档（sync/async/exit，按调用显式取舍一致性/吞吐）— 持久化强度蓝本
  - LangGraph Checkpointer SPI + 官方契约测试包（与不周山五 SPI + test-jar 模式同向，先例背书）
  - LangGraph `put_writes`「已完成写入不重放」思想 — 幂等去重蓝本
  - OpenAI Agents SDK Session 多后端 + 装饰器式包装 — 存储扩展形态参考
  - 底座事实依据：02 号票研究（`research/spring-ai-baseline` 分支）
- **模块时序**：路线图「03 M1 先行 → 05 依赖恢复语义与 SPI 现状梳理」；本机制在 03 M1 之后，需先梳理既有租约/写路径/悬空修复现状（本 Spec 已盘点）。租约心跳与 06 优雅停机共用会话生命周期关注点，实现期对齐。
- **实现期须核验的开放项**：
  1. **租约心跳**：当前 `DefaultAgentRuntime` 取租约后不续约（主代码无 `renew()` 调用方）——长轮次会被误判崩溃。心跳是恢复信号可靠性的前提，实现期须补并固定心跳间隔 vs TTL（与 06 对齐）。
  2. **去重原子语义各后端落地**：put-if-absent / reserve-then-fill 在 jdbc（行级锁/影响行数）、redis（Lua）、内存（CAS）的具体写法 + 契约断言。
  3. **「被中断轮次」判定信号**：以 `DanglingCallRepairer` 的中断标注 + 「历史结尾无终结性助手回复」为准，实现期验证边界（如多轮工具循环中途崩溃、流式崩溃）。
- **未来集成点（设计时预留，不在 M1 实现）**：去重记录与 09 工具结果缓存分工（去重=正确性、缓存=效率）；自动重驱动崩溃循环兜底复用 03/04 熔断；`EXIT` 档 flush 与 06 drain 联动；持久化档位与 21 多租户、22 数据生命周期（TTL/全链路删除）共用 per-session 存储扩展。
