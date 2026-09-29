# Spec 8010 — GraphColoring 图着色（effort #8010，V11）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8021–V8022，impl 2312）。
> 借鉴：Welsh & Powell 1967（寄存器分配/排课表同源思想）。

## Problem Statement

资源冲突分配的病：最优色数是 NP-hard——**Welsh–Powell
度降序贪心给 (Δ+1) 上限内的可行着色**，工程面要的是
有界可行而非理论最优。

## Solution

`GraphColoring`（core/concurrent，静态工具面）：节点按度
降序（并列按编号升序 canonical）逐个分配「邻居未占的最小
色」；`colors` 每节点色号 + `colorCount` 色数读数；自环/
节点越界/负权边 fail-fast；确定性纯函数（同图同色分配）；
Δ+1 上限性质承诺（不是最优色数——诚实边界明示）。

## Testing Decisions

- 手锚（偶圈 2 色/奇圈 3 色/完全图 K4=4 色/星形 2 色/
  独立集全 0 色——canonical 编号序）；色数 ≤ Δ+1 + 邻异色
  性质 200 随机图钉住；确定性双跑全等；fail-fast。

## Out of Scope

- 不追求最优色数（NP-hard 明示）；不做 Dykstra 改良。

## Further Notes

- 与 BipartiteChecker（8008）同族不同面：二染色判定面 vs
  一般 k 着色构造面。
- 里程碑：V11/50（22%）。
