# 607 — 黄金轨迹 payload 归一化

> 借鉴：[approvals / ApprovalTests](https://github.com/approvals/ApprovalTests.Java)——易变值哨兵化后做结构黄金断言。
> 来源：F 会话第 8 轮 = effort #600 / [T864](../../.wayfinder/tickets/T864-golden-normalize-shape.md) / [T865](../../.wayfinder/tickets/T865-golden-normalize-verify.md) / impl 460。

## 背景

黄金轨迹断言（spec 32）覆盖类型序/间隔/计数/payload 谓词，但 payload **值级**断言缺席——UUID、时间戳、epoch 毫秒每轮不同，直接比对必 flaky；用谓词逐字段放行又丢失结构约束。

## 目标

`EventSequenceAssert` 增归一化断言面：易变值 → 稳定哨兵后全等比对。

## 非目标

- 不改既有谓词断言语义。
- 不做数值容差归一（近似值比较归谓词面）。

## 设计

- 哨兵：`<uuid>`（UUID 正则）、`<instant>`（ISO-8601 时刻）、`<duration>`（ISO-8601 时长）、`<epochMs>`（13 位数字）、`<epochSec>`（10 位数字）。
- `normalizeValue` 深层递归 Map/List；键保序（LinkedHashMap）。
- `assertPayloadNormalized(type, expected)`：首个命中事件归一化后与期望全等，失败信息带期望/归一/原始三列。
- 发布于 core test-jar（testsupport 面，不进 api-surface 快照）。

## 测试

3 用例：单值哨兵化与稳定值原样、嵌套递归、emitEvent 端到端。

## 兼容性

test-jar 纯增量方法；既有黄金轨迹零变化。
