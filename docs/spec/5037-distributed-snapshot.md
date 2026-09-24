# Spec 5037 — Distributed Snapshot 一致快照（effort #5037，S38）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6175–T6176，impl 2188）。
> 借鉴：Chandy-Lamport 标记快照（Flink barrier/Akka 同源思想）。

## Problem Statement

分布式状态观测的病：各节点各自采样拼不成全局一致瞬间
（迁移中的消息不知归属——鬼影状态）——**标记法一致
快照面**缺失。

## Solution

`DistributedSnapshot`（core/observability，确定性事件驱动
模拟面）：

- 双向信道 FIFO 图；0 号发起：记自身状态并向全部出边发
  标记；
- 进程**首次**收标记：记自身状态+该信道封口+向全部出边
  续发标记；每信道标记到达即封口该信道记录——信道记录=
  发起者快照后发出、标记到达前在该信道的报文（在途消息
  归信道不归进程——不丢不重）；
- `result()`：进程状态 + 信道在途全集（未完成拒出口）；
- fail-fast：进程越界/自环/重复边、空报文、空信道收报、
  重复发起、未发起收标记、已封口重复收标记、未完成出
  结果。

## User Stories

1. 作为观测作者，全局状态=各进程记录+各信道在途——
   一致切片可审计。
2. 作为恢复作者，快照点可复现——确定性事件序列回放。

## Testing Decisions

- 教科书两进程场景钉住：快照前 m1 归进程消费、快照后
  m2 归信道记录（0->1:[m2]、1->0:[]、状态 {0:1,1:2}）；
  标记后报文不入记录；未完成拒出结果；重复发起/已封口
  重复标记/未发起收标记/空信道收报 fail-fast。

## Out of Scope

- 不做真实网络/并发（确定性事件驱动语义面）；不做多快照
  并发（单快照语义）；不做向量时钟融合（VectorClockOrder
  已覆盖因果序面）。

## Further Notes

- 与 SessionArchiver（归档导出）同族不同面：本地全量导出
  vs 分布式一致瞬间。Wave 7 第二件。
- 里程碑：S38/50（76%）。
