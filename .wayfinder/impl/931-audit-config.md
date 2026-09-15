# 931 — GuardAuditConfig fromGuardMap 解析全分支补测（R29）

**What to build:** GuardAuditConfigFromGuardMapTest（8 用例）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] GuardAuditConfigFromGuardMapTest（8 用例：null/非 Map 回退/全字段往返/blank 与非法容量回退/负 min-verify 回退/blank key-dir 忽略/非法 KeyFile 过滤/public-key 可选缺省）
- [x] spec 1228 + README 行
- [x] 验证：定向绿 + GuardAuditConfig 分支 17%→92% 入账 + guard 全量绿

## Done

验证：定向 8 用例全绿；guard 406 用例全量绿；GuardAuditConfig 分支 92%。commit 见本轮 `test(guard)` 提交。
