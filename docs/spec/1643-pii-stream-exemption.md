# 1643 · 流式 PII 类型级豁免（820 第四消费者）

> 来源：N 会话 R44（effort #1643 / T2437–T2438 / impl 1196）。豁免族四消费者
> 闭环：危险工具（1624）→ PII 输出（1627）→ PII 输入（1632）→ PII 流式（本 spec）。

## Solution

`PiiStreamRedactionHook` 4 参构造 +exemptions：`replyStreamFilter()` 创建每轮
窗口过滤器时做类型级生效集剔除（subject=`type:TYPE`，mechanism 沿用
`pii-redaction`——流式与输出同域）。
**诚实边界**：StreamTextFilter SPI 的 chunk 流不携带会话上下文——会话级豁免
不适用于流式面（如实标注，不造会话推断）。

## Testing Decisions

- `PiiStreamExemptionTest` 两断言（chunk 化喂入与既有流式测试同型）：
  type:CN_PHONE 豁免 → EMAIL 跨 chunk 照脱 + 手机号原文保留；无豁免双脱。
- 回归：guard 全量 372 用例。

## Out of Scope

- 流式 SPI 的会话上下文扩展（filter 携带 sessionId——core SPI 变更独立裁决）。
