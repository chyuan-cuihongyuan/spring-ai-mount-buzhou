# 626 — 事实衰减装配扩散

> 来源：F 会话第 27 轮 = effort #600（spec 604 原语的装配扩散；原语自 memory 移驻 core）/ [T902](../../.wayfinder/tickets/T902-fact-decay-assembly-shape.md) / [T903](../../.wayfinder/tickets/T903-fact-decay-assembly-verify.md) / impl 479。

## 背景

DecayingFactStore（604）在 memory 模块；FactStore 生产构造点在 GuardModule（guard）——模块互斥下无法接线。

## 目标

移驻 core.internal.memory（DefaultFactStore 同址）+ GuardModule.Builder.factDecay + yml `buzhou.guard.fact-decay.{half-life-turns,floor}`。

## 非目标

- 不改 604 衰减语义（移驻零逻辑变化）。

## 设计

Builder 声明即包装；half-life-turns 缺省无包装；floor 默认 0.25。

## 测试

装配 2 用例（yml 包装生效/缺省零变化）+ 移驻回归 5/5 + 三模块全量零回归。

## 兼容性

memory 模块 facts 包撤销（类移驻 core，快照按模块重挂——随轮再生）。
