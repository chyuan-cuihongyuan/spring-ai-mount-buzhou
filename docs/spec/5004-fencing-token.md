# Spec 5004 — Fencing Token 世代令牌护栏（effort #5004，S5）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6109–T6110，impl 2155）。
> 借鉴：Chubby fencing token（Kleppmann DDIA §8 界碑思想）。

## Problem Statement

锁失效后旧持有者的病：锁服务与存储两侧状态脱节（GC 停顿/
网络分区下旧持有者苏醒续写——锁已易主而存储不知）——**单调
令牌写侧守卫面**缺失。

## Solution

`FencingTokenGuard`（core/transaction）：

- 发令面：`acquire(lockId, holderId)` 每次持有权取得（含
  易主/重入）发**严格递增** token——单调世代；
- 守卫面：`tryWrite(lockId, token)` 三态裁决——token=当前
  已发 → ACCEPT；token<当前 → STALE_TOKEN（旧持有者苏醒
  写被拦）；token>当前 → UNKNOWN_TOKEN（未发过的令牌
  诚实拒）；未发令 → NO_LOCK；
- release 不重置世代（fencing 跨释放持久——旧令牌永不复用）；
- 读数：currentToken(holder 面)；嵌套 `Verdict` 不另立面；
- fail-fast：null/空 id。

## User Stories

1. 作为存储作者，锁易主后旧持有者的迟到写物理走不通。
2. 作为审计作者，同事件序列同裁决（确定性可回放）。

## Testing Decisions

- 易主拦截图景（A token1 锁过期 → B token2 → A 写 STALE/
  B 写 ACCEPT）；未来 token UNKNOWN；无锁 NO_LOCK；release
  后旧 token 仍拒（世代不重置）；多锁独立；畸形 id fail-fast。

## Out of Scope

- 不做锁租约/续租面（归租约族既有件）；不做存储接入适配
 （本件是守卫语义面）；不做 holder 身份鉴权。

## Further Notes

- 与 CommitGraph（世代号血缘剪枝）同族不同面：图谱血缘 vs
  锁安全守卫；与 SequenceFence（webhook 序栅）不同面：跨
  客户端锁界碑 vs 流内序栅。Wave 2（一致性共识族）开波。
- 里程碑：S5/50（10%）。
