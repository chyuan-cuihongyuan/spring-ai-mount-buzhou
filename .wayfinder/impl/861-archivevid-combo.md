# 861 — 归档×evidence 回查联动组合测试轮

**What to build:** ArchiveEvidenceComboTest——归档后回查联动 + 双读面守恒。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ArchiveEvidenceComboTest（归档/回查/守恒/隔离三测）
- [x] spec 1109 + README 行

## 勘误

测试落位 buzhou-memory（memory 依赖 core，SessionArchiver 可引用；core test 反向不可引）——验证命令 `mvn -pl buzhou-memory -am test`。

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='ArchiveEvidenceComboTest'` 全绿。commit 见本轮 `test(core)` 提交。
