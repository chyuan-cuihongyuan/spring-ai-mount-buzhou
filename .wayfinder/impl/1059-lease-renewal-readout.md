# 1059 — 租约续期健康读面

**What to build:** SessionLeaseGuard 增量（failures/minRemainingAtRenewal 水位/lastRenewalAt + renewalStats() 嵌套 RenewalStats + recordRenewalSuccess 提取）+ 四测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] guard 记账增量（两成功路径共用 recordRenewalSuccess；两失败路径计数）
- [x] LeaseRenewalReadoutTest（哨兵/水位+时刻/失败终态/单调）
- [x] spec 1406 + README 行（internal 包——无新公共类型，快照面不变）

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='LeaseRenewalReadoutTest'` 4/4 绿 + LeaseRenewFenceTest 回归 7/7 绿。
