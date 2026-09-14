---
id: T2403
title: R27 中期对账审计的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2402
created: 2026-09-15
---

## Question

N 会话第 27 轮（过半审计）：26 轮改动的质量基线如何验？

## Resolution

**隔离 worktree 全仓 verify**（主工作区被并行会话构建竞争/半成品挡路——
历史 32 分钟挂死教训）+ 工件双向对账。发现：① API 快照非破坏新增 10 类
（6 个 Holder + 4 个新公开类）未入档——worktree 内 -am 再生（主工作区被
observability 半成品编译错挡住）+ api-surface.md 同步；② spec 1622 悬空
引用补档；③ 全部工件实存。
