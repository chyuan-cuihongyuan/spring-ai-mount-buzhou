# 1205 — K 会话周期对账轮 R6（R7 起对账/雾区交替）

> 来源：K 会话第 6 轮 = effort #1205（[T1816](../../.wayfinder/tickets/T1816-k-audit-r6-shape.md) / [T1817](../../.wayfinder/tickets/T1817-k-audit-r6-verify.md) / impl 908）。方法论：**Google SRE Production Readiness Review**——台账与仓库现实的独立周期性核对；「过程工件不核对即漂移」。

## Problem Statement

K 线四步证据驱动批次（R1 零覆盖清零 → R2 低覆盖批次 → R3 漏网契约 → R4/R5 判据收紧尾巴与跨模块复核）收官后，五轮产出的工件链（票 ↔ spec ↔ impl ↔ map ↔ README）与全仓健康需要一次独立核对——五轮间并行会话扩至 M/N 两线、发生过他线卷入提交（T1815 被 cc084a62 卷入）与工作区编译竞争（CoreApiJavadocCoverageTest 半成品），漂移风险实证存在。

## 目标

- **全仓 `mvn verify`（隔离 worktree，CI 等价门）**：16 模块 + JaCoCo ≥70% 线 + enforcer 依赖收敛 + SpecCoverage 门全过；无 Docker 口径下容器测试按设计 skip。
- **工件链五项对账**：spec 1200–1205 存在且 README 各一行；票 T1801–T1817 全 closed；impl 903–908 全 done；map Decisions so far 全登记；K 线 R2–R5 增量 33 用例回归绿。
- 发现的不一致逐条入档；主代码缺陷另列票（不在对账轮夹带）。

## 实现决策

- 全仓 verify 在 `.scratch/k-wt` 隔离 worktree 执行（R4 确立的证据基建口径——主工作区有并行 WIP 不可用作验证基座）。
- 对账以脚本化清单执行（路径枚举 + 状态行 grep），脚本入 impl 台账可重放。
- 雾区清单刷新：report-aggregate 聚合报告 / 分支覆盖（BRANCH）维度两个 fog 项保持；「收紧判据跨模块复核」已收敛除名。

## 测试决策

- 验收门 = 命令退出码 + 脚本清单输出，非感觉；失败项逐条列出，不许「基本绿」。
- 先例：J 会话 R44/R50 对账轮（五类工件对账 + 隔离 worktree 双证）。

## 兼容性

纯验证轮：主代码零变化、公共 API 面零变化（若验证显形缺陷，修复单列票独立 commit）。

## Out of Scope

- 不在本轮实施 report-aggregate / BRANCH 维度（雾区另轮裁决）。
- 不重跑 nightly perf 组。
- 不触碰 I/J/M/N 会话在跑主题与号段产物。

## Further Notes

- R7 起 K 线节奏改为「对账轮 / 雾区裁决轮」交替：对账轮防漂移，雾区轮消化 report-aggregate 与 BRANCH 两个悬置裁决。
