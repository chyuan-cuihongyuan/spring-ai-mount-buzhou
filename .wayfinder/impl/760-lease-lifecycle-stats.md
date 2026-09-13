# 760 — 凭证租约生命周期计数读面

**What to build:** SecretLeases renewed/renewRejected 计数 + SecretLeaseStats 统一快照 + 固定步进钟确定性测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] renewed/renewRejected 两计数（三条 renew 路径全覆盖）
- [x] SecretLeaseStats record + stats()
- [x] 既有三 getter 兼容保留
- [x] SecretLeaseStatsTest（Clock 注入：续租成功/过期拒/缺失拒/惰性剔除/幂等吊销/对齐恒等）
- [x] spec 1007 + README 行 + API 快照与 api-surface.md 增行

## Done

验证：`mvn -pl buzhou-core test -Dtest='SecretLeaseStatsTest,SecretLeasesTest'` 全绿。commit 见本轮 `feat(core)` 提交。
