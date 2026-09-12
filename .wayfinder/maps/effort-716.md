# effort #716 — Todo 陈旧度审计读数

- 会话：G 会话 700 系第 17 轮 ｜ spec [716](../../../docs/spec/716-todo-staleness.md) ｜ 票 [T1032](../tickets/T1032-todo-staleness.md)/[T1033](../tickets/T1033-todo-staleness-verify.md) ｜ impl616
- 借鉴：OpenHands（≈35K star）/Claude Code 等 agent 的 todo 纪律实践——滞留任务可视化

## 勘察（排重）

- TodoItem(id,content,status,createdTurn)+TodoStore CRUD 齐备——**滞留面**无：in_progress 挂 N turn 未完成是 agent 任务失控第一信号。
- grep stale/Staleness：零命中。

## 决定

`TodoStalenessAudit`（buzhou-tools/todo 纯函数）：analyze(items,currentTurn,staleAfterTurns)→Report——per-item age（currentTurn−createdTurn，完成项 age 仍可读）、status 计数、滞留清单（未完成且 age>阈值，按 age 降序）、oldestOpenAge、promptHint()（一行人话「N 项滞留（最老 X turn）」供注入面拼装）；阈值 ≤0 = 不判滞留（只报统计）。

## 测试

滞留判定+降序/完成项不计滞留/阈值 0 关闭/promptHint 形态/null fail-fast。

## 诚实边界

纯读数不自动清理（关闭/降级动作归 agent 或宿主）；轮次口径（非墙钟——跨会话墙钟语义归 store 层）。
