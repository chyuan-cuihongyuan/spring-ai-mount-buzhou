# 1108 — tools 五读面全矩阵组合测试轮

> 来源：J 会话第 108 轮 = effort #1108（[T1675](../../.wayfinder/tickets/T1675-toolsmatrix-shape.md) / [T1676](../../.wayfinder/tickets/T1676-toolsmatrix-verify.md) / impl 860）。纯测试轮第十五弹。tools 域组合系列收口（R82/R84/R86 两两与三面验证后全矩阵）。

## Problem Statement

R46 写/R47 读/R49 http/R51 黑名单/R56 SSRF 五读面——两两验证后**全矩阵互不串账收口**缺失。

## 目标

新增 `ToolsMatrixReadoutTest`（buzhou-tools）：五读面交叉调用后各自守恒保持 + 互不串账 + reset 独立隔离。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 更多域矩阵（按需另轮）。
