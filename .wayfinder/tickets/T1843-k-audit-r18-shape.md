---
id: T1843
title: K 会话周期对账轮 R18 形态（R13–R17 五轮后的全仓核对）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 18 轮：R13–R17 五轮（流式 harness/非流式/残余清扫/批次 4 收尾）后的既定对账节奏，对账面与验收门？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 18 轮 = effort #1217 / spec 1217 / impl 920）：沿 R6/R12 口径——

1. **对账面**：全仓 mvn verify（隔离 worktree 固定提交点）+ 工件链五项（spec 1200–1217 / README 行 / 票 T1801–T1844 / impl 903–920 / map Decisions）。
2. **R13–R17 增量回归**：新增 7+13+6+11+5 = 52 用例回归绿。
3. **验收门**：verify exit 0；对账清单全 OK；不一致逐条入档。
