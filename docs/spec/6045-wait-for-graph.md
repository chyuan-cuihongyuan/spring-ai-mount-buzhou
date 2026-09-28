# Spec 6045 — WaitForGraph 等待图死锁检测（effort #6045，T45）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6289–T6290，impl 2245）。
> 借鉴：DB2/SQL Server 锁管理器 lock-wait graph 思想。

## Problem Statement

死锁处置的病：事后全图扫描（死锁扩大化才被发现）与
全局超时轮询（无差别惩罚无辜等待者）——**增量加边
即时环检测 + 受难者裁决面**缺失。

## Solution

`WaitForGraph`（core/concurrent）：

- 节点=事务，有向边 waiter→holder 表示「等待」；环即
  死锁——加边即时回报规范环（起点/邻接按 id 升序 DFS +
  环内最小 id 旋转到首位——同图同环完全确定）；
- 受难者 = 环内最大 id（「最年轻者回滚代价最小」约定）；
  removeNode 打断（出入边全清、计数守恒）；
- addNode/removeNode/nodeCount/edgeCount/hasDeadlock/
  deadlockVictim 读数；未注册端点/自环/重复节点/缺席
  摘除 fail-fast。

## User Stories

1. 作为锁管理器作者，加边瞬间得知死锁并裁剪受难者。
2. 作为审计作者，同图同环——检测完全可复现。

## Testing Decisions

- 无环链零误报；二环/三环带入口路径规范环逐值钉住；
  打断后复原再扩展；fail-fast 四路。

## Out of Scope

- 不做代价模型受难者选择（固定最年轻约定）；不做并发安全。

## Further Notes

- 与 TarjanSccFinder（同包）同族不同面：离线全图强连通
  分量 vs 增量加边即时环检测+受难者裁决。
- 勘误：removeNode 初版漏减被摘节点出边计数 + 规范环未
  旋转双缺陷由合同钉住修正。
- 里程碑：T45/50（90%）。
