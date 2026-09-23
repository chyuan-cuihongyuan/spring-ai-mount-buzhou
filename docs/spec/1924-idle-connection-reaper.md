# Spec 1924 — 空闲连接收割（effort #1924，R125）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3047 替补槽，
> impl 1525）。借鉴：HikariCP（20K+ 星）idle reaper——连接空闲超过
> maxIdle 即收割（归还池/关闭）：边界含上（恰 maxIdle 即收），
> 活跃连接永不动。

## Problem Statement

长生命周期连接（工具进程/MCP 通道）闲置堆积无人管：收割判定散落
在各使用方——「哪些连接该收、闲置多久算闲」缺统一判定面。

## Solution

`IdleConnectionReaper`（core/concurrent，静态纯函数）：

- `reapCandidates(lastUsedByConn, nowMillis, maxIdleMillis)`：闲置
  ≥ maxIdle 的连接键列表（边界含上，闲置最久的排前——优先收割）；
- `idleMillis(lastUsed, now)`：闲置时长读数。

## User Stories

1. 作为连接池作者，maxIdle 30s：闲置 45s 的连接入选、30s 恰到
   入选、29s 留——边界有明确口径。
2. 作为收割执行者，闲置最久排前——有配额时先收最旧的。
3. 作为排障者，idleMillis 读数接观测——闲置分布可见。

## Implementation Decisions

- 纯函数零状态；lastUsed/now ≥ 0、now ≥ lastUsed、maxIdle ≥ 1
  fail-fast；活跃连接（闲置 0）永不入选。

## Testing Decisions

- 混合闲置一例（入选/不入选/恰边界）；排序最旧优先；畸形三型
  fail-fast。

## Out of Scope

- 不做真实连接关闭（归池）；不做连接建立。

## Further Notes

- 与 PoolSizeHeuristic（R89 容量公式）互补：那是开多少，这是
  收哪些。
