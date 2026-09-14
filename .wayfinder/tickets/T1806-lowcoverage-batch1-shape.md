---
id: T1806
title: 低覆盖类批次 1 选题与补测形态（PolicyGateHook × RecallSearchTool）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 2 轮：R1 清零零覆盖后，R2+ 首批「低覆盖类」（覆盖率 <50% 且 LINE_MISSED ≥ 10）证据与选题如何裁决？补测形态沿什么先例？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 2 轮 = effort #1201 / spec 1201 / impl 904）：

1. **证据源沿 R1 口径延伸**：低覆盖 = 覆盖率 <50% 且 `LINE_MISSED>=10`（R1 零覆盖判据的相邻档）。2026-09-14/15 报告扫描：guard `PolicyGateHook`（cov=6 / mis=11，仅装配触达）+ memory `RecallSearchTool`（cov=26 / mis=30，完全无直测文件）。本批 = 这 2 类；core 低覆盖靶点待本轮 core 复扫后归入批次 2（证据不齐不开票——证据源唯一纪律）。
2. **guard PolicyGateHook 补测面**：三态裁决映射（allow→CONTINUE / deny→Block 附「策略拒绝」/ escalate→Block 附「等待人工确认（策略升级）」审批文案）+ 结构化 input 组装断言（principal=agentName / arguments 透传 / humanApproved=false / FIDES taint label 映射：state 前缀 UNTRUSTED → `taint=UNTRUSTED`，否则 TRUSTED）+ policy.decided 事件字段（action/reason/revision，null revision → "" 空串占位）+ 指标 `buzhou.guard.checks` 按 outcome 分桶。借 **OPA（Open Policy Agent）「结构化 input → decision + reason + provenance」** 裁决合同测试思想：决策入口的输入形状本身就是合同。
3. **memory RecallSearchTool 补测面**：text/time/embedding/hybrid 四模输出格式（命中行 turn/role/score/id/摘要）+ 摘要空白归一与 160 字符截断 + 无命中/缺 sessionId/非法 mode 三类文案分支 + EMBEDDING/HYBRID 未注入 provider 显式降级（借 **Elasticsearch partial-results「降级要显式提示而非静默半结果」** 思想）+ limit 默认 10 与 fromTurn/toTurn 轮次窗。embedding fake 用确定性词包向量化（EmbeddingProviderTest R1 先例，char-bucket）。
4. **形态沿先例**：JUnit 5 + AssertJ 静态导入、无 Mockito（PolicyEngine 是 functional interface → lambda stub 捕获 Input；ToolCallContext 用 DefaultToolCallContext + HookEnvironment，TaintLifecycleStatsTest 先例；录制型 metrics 用 CapturingMetrics implements BuzhouMetrics + @AfterEach reset，ToolDurationTimerTest 先例）；测试与被测类同包。
5. **边界**：不改主代码行为；若补测显形真实缺陷则单列票独立修复（R1 T1803–T1805 纪律）。
