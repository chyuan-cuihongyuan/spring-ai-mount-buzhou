# 1201 — 低覆盖类批次 1：PolicyGateHook × RecallSearchTool（R2）

> 来源：K 会话第 2 轮 = effort #1201（[T1806](../../.wayfinder/tickets/T1806-lowcoverage-batch1-shape.md) / [T1807](../../.wayfinder/tickets/T1807-lowcoverage-batch1-verify.md) / impl 904）。方法论：coverage-guided test completion 的相邻档延伸——零覆盖清零后清「低覆盖」档（覆盖率 <50% 且 LINE_MISSED ≥ 10），借 **Google Testing Blog「coverage 找洞、行为语义补洞」** 分层缺口思想。

## Problem Statement

R1 清零零覆盖后，低覆盖档仍藏「测了但只擦过边」的面：guard `PolicyGateHook`（cov=6 / mis=11）只有装配级触达，其三态裁决映射、FIDES taint label 组装、policy.decided 事件字段、指标分桶四个行为面从未被断言；memory `RecallSearchTool`（cov=26 / mis=30）完全无直测文件，其四模输出格式、摘要截断、降级文案、缺上下文失败分支全靠 `RecallSearch` 引擎测试间接路过。这些类是策略门（安全面）与模糊召回（记忆面）的**用户可见出口**——输出文案与事件形状即合同，无直测意味着文案回归无防线。

## 目标

- **guard PolicyGateHookTest**（8 断言面）：
  - 三态裁决映射：allow → `HookResult.CONTINUE`；deny → `Block` 且 reason 以「策略拒绝：」前缀；escalate → `Block` 且含「等待人工确认（策略升级）」审批文案与重试指引；
  - 结构化 input 组装（OPA「input→decision+reason」合同思想）：principal=agentName、toolName、arguments 透传、humanApproved=false、labels 仅 `taint` 一键；
  - FIDES taint 映射：state `taint.context` 值前缀 `UNTRUSTED` → label `UNTRUSTED`；无 state / 非 UNTRUSTED 前缀 → `TRUSTED`；
  - `policy.decided` 事件：sessionId/toolName/action/reason 字段；revision 非 null 透传、null → 空串占位（provenance 面不缺字段）；
  - 指标 `buzhou.guard.checks` 按 outcome 三桶（allow/deny/escalate，Action 名小写——补测显形源注释「allowed|blocked|escalated」过期失真，[T1808](../../.wayfinder/tickets/T1808-policygate-metric-comment.md) 单列修正）各计一次（CapturingMetrics 先例 + @AfterEach reset）；
  - name/order 常量合同（275：taint 写门 250 之后、HITL 门 300 之前）。
- **memory RecallSearchToolTest**（10 断言面）：
  - text 模式命中行格式：`命中 N 条（mode=text）` + turn/role/score/id/摘要五字段；
  - 摘要空白归一（换行/制表折叠单空格）与 160 字符截断加「…」；
  - 无命中文案 `[recall_search 无命中] mode=…`；
  - 缺会话上下文失败分支：ToolContext 为 null / context 空 Map / sessionId 字面 `"null"` 三入口同文案；
  - EMBEDDING/HYBRID 未注入 provider 显式降级文案（Elasticsearch partial-results「降级要显式而非静默半结果」思想）；
  - EMBEDDING/HYBRID 注入 provider 后可用（确定性 char-bucket 词包 fake，EmbeddingProviderTest R1 先例）；
  - time 模式按轮次倒序、score 恒 1.00；
  - 非法 mode 返回 `[recall_search 失败]` 前缀（异常路径显式文案而非抛穿）；
  - limit 默认 10、fromTurn/toTurn 轮次窗过滤；
  - 单参 `call(String)` 委托双参（无上下文 → 失败文案）。

## 实现决策

- 纯测试增量：两测试文件与被测类同包（包私有可达），主代码零变化、公共 API 面零变化。
- stub 形状沿仓库惯例（无 Mockito）：PolicyEngine 是 functional interface → lambda stub 捕获 `PolicyDecision.Input`；ToolCallContext 用 `DefaultToolCallContext` + `HookEnvironment`（TaintLifecycleStatsTest 先例），taint 经 `InMemorySessionStateStore.put` 预置 StateEntry；录制型指标 `CapturingMetrics implements BuzhouMetrics` + `BuzhouMetricsHolder.install/reset`（ToolDurationTimerTest 先例）；事件经 `HookEnvironment.bindEventPublisher` 收集断言。
- embedding fake：char-bucket 定长向量（`v[c % 8] += 1`），语义重叠（kafka offset × kafka offset）余弦高分、无关文本低分——确定性无随机。

## 测试决策

- 好测试标准：只断言外部可见行为（返回值文案、事件字段、指标分桶、捕获的 engine 输入），不断言内部调用序列；每个断言对应一条可陈述的合同语义。
- seam 全取既有最高面：hook 走 `beforeTool(ctx)` 单点；工具走 `call(input, toolContext)` 单点；指标走全局 Holder（测试纪律 @AfterEach 还原 no-op）。
- 先例：TaintLifecycleStatsTest（ctx stub / state 预置）、ToolDurationTimerTest（CapturingMetrics）、EvidenceLookupToolTest（InMemoryMessageStore + BuzhouMessage 构造）、EmbeddingProviderTest（词包 fake）。
- 验收门：guard / memory 定向测试绿 + JaCoCo 复扫两类脱离低覆盖档（剩余 miss <10 或覆盖率 ≥50%）。

## 兼容性

纯测试增量：主代码零变化、配置零变化、既有测试零改动。

## Out of Scope

- 不追 100% 覆盖；防御分支边缘行（如 null state 的 HookContext 现行构造面不可达）不硬凑。
- 不做 core 低覆盖批次（待本轮 core 复扫证据归批后入 R3+）。
- 不触碰 I/J 会话号段产物与在跑主题。

## Further Notes

- RecallSearchTool 的 sessionId 提取 `String.valueOf(getOrDefault(KEY, null))` 对缺失键产生字面 `"null"` 字符串——工具内已双防（`== null || "null".equals`），三入口同文案断言正是锁住这一合同。
- PolicyGateHook 的 ESCALATE 文案是 HITL 审批通道的用户指令面（「请用户审批后重试」）——文案回归 = 审批流程回归，值得逐字锁定前缀。
