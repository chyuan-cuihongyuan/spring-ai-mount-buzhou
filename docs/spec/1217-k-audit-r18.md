# 1217 — K 会话周期对账轮 R18

> 来源：K 会话第 18 轮 = effort #1217（[T1843](../../.wayfinder/tickets/T1843-k-audit-r18-shape.md) / [T1844](../../.wayfinder/tickets/T1844-k-audit-r18-verify.md) / impl 920）。方法论：沿 R6/R12 的 SRE Production Readiness Review 口径。

## Problem Statement

R13–R17 五轮（流式 harness 从零搭建/非流式对称面/残余清扫/批次 4 收尾）新增 5 个测试文件 52 用例，需独立核对台账与仓库现实一致性并确认全仓健康。

## 目标

- 全仓 `mvn verify`（隔离 worktree 固定提交点）：16 模块 + 三门。
- 工件链五项对账（spec 1200–1217 / 票 T1801–T1844 / impl 903–920 / map / README）。
- R13–R17 增量 52 用例回归确认。

## 实现决策

- 验证基座 = `.scratch/k-audit-wt`（强制重置到干净 HEAD 后执行——上轮实证 worktree 会被并行会话留脏，reset 前置为对账轮标准步骤）。

## 测试决策

- 验收门 = 命令退出码 + 清单输出；无 Docker 口径容器测试按设计 skip。

## 兼容性

纯验证轮。

## Out of Scope

- ObservabilityAdvisor 剩余 70 missed 细粒度（R19+ 候选）。
- 他线在跑主题。

## Further Notes

- R18 后 K 线节奏：对账轮与专项批次轮持续交替，直至 150 轮长跑目标。
