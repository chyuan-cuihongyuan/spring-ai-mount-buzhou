# 1627 · PII 脱敏豁免双粒度（spec 820 第二消费者）

> 来源：N 会话 R28（effort #1627 / T2405–T2406 / impl 1180）。

## Problem Statement

PII 脱敏的两类生产痛点：① 某类型检测误报率高（如手机号正则匹配到订单号），
每次人工复核成本高；② 某可信数据源的输出被无差别脱敏（内部已合规工具）。
豁免登记（spec 820）需要第二消费者覆盖这两个面。

## Solution

`PiiRedactionHook` 构造器 +exemptions（null = 零行为；GuardModule 装配传入同
registry 实例）：
- **工具级**：subject=工具名——afterTool 开头短路（该工具输出原样透传）；
- **类型级**：subject=`type:TYPE`（如 type:CN_PHONE）——该类型从当次生效集
  剔除，其余类型照脱（detector.redact(content, effectiveTypes)）。
两者均有时限（grant 的 until）、可撤销、过期恢复脱敏。

## Testing Decisions

- `PiiExemptionTest` 三断言：工具级豁免原样透传；类型级豁免 EMAIL 脱敏 +
  CN_PHONE 原文保留；无豁免基线全脱。
- 回归：guard 全量 359 用例。

## Out of Scope

- 自定义规则的豁免粒度（custom rule 名——若需要第三粒度再立项）。
- 输入侧（PiiInputRedactionHook）/流式（PiiStreamRedactionHook）的征询
  （同款构造器扩参——按痛点出现再接）。
