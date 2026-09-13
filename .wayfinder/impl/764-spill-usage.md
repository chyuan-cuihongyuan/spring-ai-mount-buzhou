# 764 — spill 容量水位读面

**What to build:** SpillUsage record + DiskSpillStore.usage() 快照（单 walk 计字节与条数）+ TempDir 确定性测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SpillUsage record（totalBytes/entryCount）
- [x] usage() synchronized 快照（与配额守卫同口径 walk）
- [x] SpillUsageTest（空仓/两次 store/delete 回落）
- [x] spec 1011 + README 行 + API 快照增行

## Done

验证：`mvn -pl buzhou-spill test -Dtest='SpillUsageTest,DiskSpillStoreTest'` 全绿。commit 见本轮 `feat(spill)` 提交。
