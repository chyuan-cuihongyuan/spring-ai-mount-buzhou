# 507 — 最小可用水位闸（归档 PDB）

**What to build:** SessionAvailabilityFloor（minAvailable + liveSessions 供应商，未知 fail-open）挂 SessionArchiver.archive()——水位不足拒绝归档 + pdb-rejected 计数；restore/purge 不受闸；null floor 零变化。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SessionAvailabilityFloor（allowsArchive + fail-open 语义）
- [x] SessionArchiver 可选 floor 构造（两参默认不变）+ archive 闸 + 计数
- [x] 恰在 floor 拒绝 / 之上放行 / 未知放行 / restore 不受闸 / 零回归
- [x] spec 704 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
