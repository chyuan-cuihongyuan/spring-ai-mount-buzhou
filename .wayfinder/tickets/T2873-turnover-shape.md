---
id: T2873
title: 库存周转读面的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

库存「多活/多紧」怎么一对读数？（spec 1836 / effort #1836 / R37）

## Resolution

**供应链库存周转思想纯读面 `TurnoverReadout`（buzhou-spill）**：
turns(stock, consumed) 周转次数（无库存 -1 哨兵、零消费 0=死库存）+
depletionHorizonMillis(stock, rate) 耗尽视界（向上取整保守、零速率 -1
哨兵、零库存 0）。周转贴地=TTL 该激进，视界短=预热该启动——互为倒数
的一对处方分诊。

