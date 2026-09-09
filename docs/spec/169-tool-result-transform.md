# Spec 169 — 工具结果裁剪装饰器（effort #203）

> wayfinder map：`.wayfinder/maps/effort-203.md`（T533–T534）。借鉴：Vector VRL /
> jq——「取你要的」：工具大 JSON 包在入上下文前提炼成模型需要的字段。

## Problem Statement

工具（HTTP/DB/命令）返回常是几十 KB 的嵌套 JSON，模型只需要其中一两个字段
（status/data.rows[0]）：限幅器（spec 31）只会截断保底——截掉的可能恰是要点，
留下的可能是噪声；宿主每个工具手写后处理又散落业务代码。

## Solution

`TransformingToolCallback`（core/exec，装饰器）：

- `wrap(callback, UnaryOperator<String> transform)`——工具结果在返回前过
  变换（提字段/抽段落/清噪——宿主声明式给定）。
- **fail-open 契约**：变换抛异常 / 返回 null / 返回空白 → <b>原样返回</b>
  ——变换是尽力提炼，绝不丢数据（截断护栏仍在下游兜底）。
- 定义透传（名称/schema/description 不变——装配面零感知，装饰器家族同款）。

## User Stories

1. 作为宿主，几十 KB 的天气 JSON 提成「城市/温度/摘要」三行——上下文省 99%
   且模型拿到的全是信号。
2. 作为宿主，变换写错（字段名笔误/格式变化）时拿到原始结果——fail-open
   不丢数据，只是省得少了。
3. 作为模型，我看到的是提炼后高密度结果，不再在噪声里找字段。

## Implementation Decisions

- 变换签名 String→String（结构化变换宿主自行 JSON 解析——框架不猜格式）。
- 与 memo/retry 可叠加（装饰顺序归宿主）。

## Testing Decisions

- 变换生效（提字段断言）；变换抛异常回退原文；返回 null/空白回退；
  组合两个变换（内层抛异常时外层仍拿到原文）；定义透传。

## Out of Scope

- 内置变换库；yml 声明式；流式。

## Further Notes

- 结果治理三层：裁剪提炼（本轮，宿主主动）→ 限幅截断（31，保底）→ spill
  落盘回读（溢出）。
