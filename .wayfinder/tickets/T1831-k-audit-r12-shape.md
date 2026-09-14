---
id: T1831
title: K 会话周期对账轮 R12 形态（第二次全仓 verify + 工件链对账）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 12 轮（R8–R11 四轮之后的既定对账节奏）：对账面与验收门如何定义？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 12 轮 = effort #1211 / spec 1211 / impl 914）：沿 R6 口径——

1. **对账面**：全仓 mvn verify（隔离 worktree，固定提交点）；工件链五项（spec 1200–1211 / README 行 / 票 T1801–T1832 / impl 903–914 / map Decisions）。
2. **R8–R11 增量回归确认**：新增 22+11+17+11 用例回归绿。
3. **验收门**：verify exit 0（无 Docker 口径：容器 skip）；对账清单全 OK；不一致逐条入档。
