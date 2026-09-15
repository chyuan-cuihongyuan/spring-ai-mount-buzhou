---
id: T1853
title: K 会话周期对账轮 R23 形态（R19–R22 四轮后的全仓核对）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 23 轮：R19–R22 四轮（R19 流式语义定向/R20 快照预算+extraKeys/R21 BuzhouMemoryAdvisor/R22 HookAdvisor）后的既定对账节奏，对账面与验收门？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 23 轮 = effort #1222 / spec 1222 / impl 925）：沿 R6/R12/R18 口径——

1. **对账面**：全仓 mvn verify（隔离 worktree 固定本地 HEAD）+ 工件链五项（spec 1200–1222 / README 行 / 票 T1801–T1854 / impl 903–925 / map Decisions）。
2. **R19–R22 增量回归**：新增 5+3+8+9 = 25 用例回归绿。
3. **验收门**：verify exit 0（无 Docker 口径容器 skip）；对账清单全 OK；不一致逐条入档。
