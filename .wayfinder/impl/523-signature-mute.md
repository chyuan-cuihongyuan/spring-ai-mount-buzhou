# 523 — 错误签名已知问题静默标记

**What to build:** ErrorSignatures mute/unmute/mutedSignatures（MUTED_CAP=64 有界）+ top/top(kind) 排除 muted（计数照常、snapshot 原样）+ reset 连带清空。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] mute/unmute/mutedSignatures + top 排除
- [x] top 消失/snapshot 照常/回归/cap/reset/分面/不可变用例
- [x] spec 720 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
