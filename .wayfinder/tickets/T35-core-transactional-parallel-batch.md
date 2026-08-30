---
id: T35
title: core · 事务性并行批——批提交语义
type: task
status: closed
assignee: ""
blocked-by: T33
created: 2026-08-14
---

## Question

并行工具批的「事务性」到底指什么？**研究修正**：LangGraph（39,627★）superstep **并非**「任一失败→整批回滚」，实为 pending-writes 半事务（同批互不可见快照隔离、失败者写不提交、**兄弟成功写保留**、恢复时成功写重放失败者重跑、外部副作用无回滚）——第一轮研究的表述须修正后落地。

## 待定决策（研究推荐已备）

1. 并行工具批 =「**批提交**」：全部成功才把整批 ToolResponse 追加 history 并落 Completed-Turn——采纳。
2. 任一失败→失败者走 `ToolErrorFeedback`、成功者结果**暂存批记录**；回喂**策略显式可配**（全部回喂 / 仅失败回喂）而非隐式——采纳（诚实对齐 LangGraph 真实语义，不宣称副作用回滚）。
3. 暂存批记录落在 T33 的 ToolCallLog（同一证据层）——采纳。

依据：`docs/research/oss-perfect-tier23.md` §2.6（2–4 天，ROI 中；T33 打底后很便宜）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §core-7**（用户常设授权 2026-08-14 ratify、可推翻）。批提交语义（LangGraph 修正版）：全成才入 history、成功者暂存、回喂策略显式可配、不谎称副作用回滚。
