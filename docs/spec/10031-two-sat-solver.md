# Spec 10031 — TwoSatSolver SCC 缩点 2-SAT（effort #10031，X32）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10063–X10064，impl 2434）。
> 借鉴：Aspvall–Plass–Tarjan 1979 思想——CLRS 思考题/sedgewick 同源

## Problem Statement

2-CNF 可满足性——蕴涵图缩点判 CASP 的线性直读面（布尔可满足的最简多项式子族）。

## Solution

TwoSatSolver（core/concurrent）：solve(n,clauses)——子句 (a∨b) 引 ¬a→b、¬b→a 双蕴涵边（±v 有符号字面量）；KosarajuScc（10030）缩点+凝聚图 Kahn 确定性拓扑序；赋值 x=真 iff 正字量分量拓扑后于负字量分量；正负同分量判 UNSAT（Optional.empty）。

## Testing Decisions

强制赋值手锚（(x0)∧(¬x0∨x1)→恒真面）+经典 UNSAT 四子句手锚+50 例植入解随机式返回赋值全满足圣像+空式平凡 SAT+确定性+fail-fast 五面。

## Out of Scope

不做 3-SAT/一般 SAT（NP 完全域不涉）；不做全部赋值枚举（一组满足赋值即合同）；不做增量子句面。

## Further Notes

与 KosarajuScc（10030）流程间自组合：SCC 判定消费面——库内组件互喂的第一例；Wave 6 图结构进阶族第二件。
