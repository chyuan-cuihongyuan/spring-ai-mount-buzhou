# 1198 — API 快照增量再生 + 冲突化解

**What to build:** 快照 worktree 再生同步 + GuardModule 并行双插去重。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] /tmp/n-close-verify 再生（+GradientLimiterHolder）+ 主区同步
- [x] GuardModule 双 dangerousTools() 去重（M 系保留）
- [x] ApiSurface 门绿（worktree）+ guard 376 用例零回归

## Done

验证：worktree ApiSurfaceSnapshotTest 绿 + 主区 guard 全量绿。
