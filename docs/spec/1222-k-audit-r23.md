# 1222 — K 会话周期对账轮 R23

> 来源：K 会话第 23 轮 = effort #1222（[T1853](../../.wayfinder/tickets/T1853-k-audit-r23-shape.md) / [T1854](../../.wayfinder/tickets/T1854-k-audit-r23-verify.md) / impl 925）。方法论：沿 R6/R12/R18 的 SRE Production Readiness Review 口径。

## Problem Statement

R19–R22 四轮（流式语义定向/快照预算+extraKeys 贯通/BuzhouMemoryAdvisor/HookAdvisor）新增 4 个测试文件 25 用例与 2 套 advisor 直测基建扩展，需独立核对台账与仓库现实一致性并确认全仓健康。

## 目标

- 全仓 `mvn verify`（隔离 worktree 固定本地 HEAD）：16 模块 + 三门。
- 工件链五项对账（spec 1200–1222 / 票 T1801–T1854 / impl 903–925 / map / README）。
- R19–R22 增量 25 用例回归确认。

## 实现决策

- 验证基座 = `.scratch/k-audit-wt`（强制重置到本地 HEAD 后执行——本地 HEAD 含 O 系 R48 对账提交，为当前最完整状态）。

## 测试决策

- 验收门 = 命令退出码 + 清单输出；无 Docker 口径容器测试按设计 skip。

## 兼容性

纯验证轮。

## Out of Scope

- ObservabilityAdvisor 剩余 ~39 missed 细粒度（R24+ 候选）。
- 他线（I/J/L/M/N/O）在跑主题。

## Further Notes

- 并行会话已扩至 O 系（1800，R48 对账轮同日运行）——多线合流的治理门震荡按 R18 口径持续入档。
