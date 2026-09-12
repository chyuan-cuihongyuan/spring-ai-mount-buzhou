# 529 — 归档 PDB yml 装配

**What to build:** buzhouSessionArchiver bean 装配 SessionAvailabilityFloor（min-available-sessions>0 且索引在场）——capped probe 计数（limit=min+1）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] bean 参数扩展 + capped probe 供应商
- [x] capped probe 三态 + 语义一致 + 全模块回归
- [x] spec 726 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
