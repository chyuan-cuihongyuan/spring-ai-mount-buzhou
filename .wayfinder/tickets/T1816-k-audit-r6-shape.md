---
id: T1816
title: K 会话周期对账轮 R6 形态（全仓 verify + 工件一致性）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 6 轮：R2–R5 证据驱动批次（零覆盖 → 低覆盖 → 收紧判据尾巴 → 跨模块复核）四步收官后，周期性对账轮的对账面与验收门如何定义？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 6 轮 = effort #1205 / spec 1205 / impl 908）：

1. **对账面**（Google SRE Production Readiness Review 思想：独立周期性核对「台账说的」与「仓库真的」）：
   - 工件链一致性：spec 1200–1205 存在且 README 各有一行 ↔ 票 T1801–T1817 全 closed ↔ impl 903–908 全 done ↔ map Decisions so far 全登记；
   - 全仓 `mvn verify`（隔离 worktree，CI 等价门：16 模块 + JaCoCo ≥70% + enforcer + SpecCoverage 门）；
   - K 线增量测试的回归确认（R2–R5 新增 33 用例全绿）。
2. **验收门**：全仓 verify exit 0（无 Docker 口径：容器测试按设计 skip）；对账脚本五项全 OK；发现的不一致逐条入档（本票内小修可捎带，主代码缺陷另列票）。
3. **节奏**：对账轮每 4–5 轮一次（沿 J 会话 R44/R50 先例），覆盖批次间的漂移；K 线后续轮次改为「对账轮 + 雾区裁决轮」交替。
