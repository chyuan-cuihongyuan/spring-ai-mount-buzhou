# 1625 · 跨会话泄漏金丝雀接线（spec 528 孤类救活）

> 来源：N 会话 R26（effort #1625 / T2401–T2402 / impl 1178）。spec 1611 普查修复
> 收尾弹：SessionCanaryRegistry（spec 528，thinkst canarytokens 思想）建成即孤。

## Solution

- `SessionCanaryHook`（guard/leak）：
  - beforeTurn：plant(sessionId)（确定性 sha256 令牌——同会话恒同，LRU 256 有界）；
  - afterModel：detect(sessionId, 输出文本)——**他会话**令牌出现即发
    `guard.session.leak-detected`（sessionId/leakedFromSession/token）+ 指标。
- 装配：`GuardModule.builder().leakCanary(salt)`（salt 非空即启用——opt-in）。
- **诚实边界（随 spec 528 原注）**：检测依赖令牌原样出现（模型改写/截断不保
  ——概率探针非隔离机制）；令牌进入会话数据的注入面归宿主（honeytoken 需
  放进数据才可被触发——hook 只登记与扫描）。

## 附：LayeredPolicy（spec 1003）裁决

纯函数诊断 record（无状态无副作用、policy 域公共原语）——**纯函数工具豁免**
不清亡不强行接线（spec 1611 普查的「疑似」清理）。

## Testing Decisions

- `SessionCanaryHookTest` 两断言：B 会话输出复现 A 令牌 → 泄漏事件一条 +
  detectedCount=1 + 自会话回显不算泄漏；种植确定性（同会话两次 size=1）。
- 回归：SessionCanaryRegistryTest 4 用例 + guard 全量。

## Out of Scope

- 令牌的自动注入面（污染输入/工具结果的取舍需独立裁决——宿主明确意图优先）。
