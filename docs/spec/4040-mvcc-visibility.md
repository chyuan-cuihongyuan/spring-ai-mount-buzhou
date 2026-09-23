# Spec 4040 — MVCC 快照可见性（effort #4040，R41）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6081–T6082，impl 2141）。
> 借鉴：PostgreSQL MVCC 快照可见性（xmin/xmax + snapshot xmax+在飞集）。

## Problem Statement

读不阻塞写的一致性读的病：读写互锁（吞吐崩）或读已提交
语义漂移（同事务内重复读漂移）——**快照一致性判定面**
缺失。

## Solution

`MvccVisibility`（core/transaction，嵌套 `Snapshot`/`RowVersion`
不另立面）：

- 事务注册面：`begin()` 递增分配 txid、`commit`/`abort` 收尾
 （未知/重复收尾 fail-fast）；
- `snapshot()`：捕获 xmax（下一未分配号）+ 在飞集 + 已知
  中止集——不可变快照；
- 可见性判定（Postgres XidInMVCCSnapshot 简化口径）：
  - 创建者可见 = xmin 有效 ∧ xmin < snap.xmax ∧ 不在飞 ∧
    未中止；
  - 删除者不可见 = xmax 缺省(0) ∨ xmax ≥ snap.xmax ∨ 在飞 ∨
    已中止（删除晚于快照/未提交/回滚都对读者不存在）；
- `repeatable read` 免费成立：同快照同判定（纯函数）。

## User Stories

1. 作为状态存储作者，长读不被并发写阻塞且读序一致。
2. 作为审计作者，同快照同可见性（确定性可回放）。

## Testing Decisions

- 六象限：先提交可见 / 快照后创建不可见 / 在飞创建者提交后
  旧照不可见新照可见 / 中止创建者永不可见 / 在飞与中止删除
  均对旧照可见、先提交删除不可见；repeatable read 双读一致；
  未知/重复收尾 fail-fast。

## Out of Scope

- 不做存储实现（本件是判定语义面）；不做 clog 截断/回收；
- 不做写写冲突检测（SSI 面）。

## Further Notes

- 与 IdempotencyKeyGuard（写侧幂等）互补：读侧一致性 vs
  写侧去重。Wave 7 收口件。
- 里程碑：41/50。
