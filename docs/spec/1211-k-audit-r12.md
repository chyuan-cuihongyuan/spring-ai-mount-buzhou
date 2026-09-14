# 1211 — K 会话周期对账轮 R12

> 来源：K 会话第 12 轮 = effort #1211（[T1831](../../.wayfinder/tickets/T1831-k-audit-r12-shape.md) / [T1832](../../.wayfinder/tickets/T1832-k-audit-r12-verify.md) / impl 914）。方法论：沿 R6 的 SRE Production Readiness Review 口径。

## Problem Statement

R8–R11 四轮（分支缺口批次 1/2/边缘清扫 + Advisor 议程定义）新增 4 个测试文件 22+11 用例与 1 行主代码修复（T1824），需独立核对台账与仓库现实的一致性，并确认全仓健康。

## 目标

- 全仓 `mvn verify`（隔离 worktree，固定提交点）：16 模块 + 三门（JaCoCo LINE ≥70% / enforcer / SpecCoverage + ApiSurfaceSnapshot）。
- 工件链五项对账（脚本清单可重放）。
- 不一致逐条入档；缺陷单列票。

## 实现决策

- 验证基座 = `.scratch/k-audit-wt` 隔离 worktree（R4 口径）；对账脚本化（与 R6 同版清单扩展 spec/票/impl 号段）。

## 测试决策

- 验收门 = 命令退出码 + 清单输出；无 Docker 口径：容器测试按设计 skip。

## 兼容性

纯验证轮。

## Out of Scope

- ObservabilityAdvisor 流式 harness（R13 议程）。
- 他线（I/J/M/N）在跑主题。

## Further Notes

- R7 BRANCH 读面台账延续：本轮重扫 observability/store-redis/observe-otel 三模块分支数（批次 1/2 提升后的复测基线）。
