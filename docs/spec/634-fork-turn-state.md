# 634 — 时间旅行 fork 回放起点 state

> 来源：F 会话第 35 轮 = effort #600（spec 602 谱系补全）/ [T918](../../.wayfinder/tickets/T918-fork-turn-state-shape.md) / [T919](../../.wayfinder/tickets/T919-fork-turn-state-verify.md) / impl 487。

## 背景

upToTurn 只在事件 payload——state 查询面（面板/模块）拿不到回放起点。

## 目标

forkFromTurn 另写 `buzhou.fork.turn`。

## 非目标

- 普通 fork 不写（无起点语义）。

## 测试

Lineage 测试扩展断言 + core 零回归。

## 兼容性

纯增量 state 键。
