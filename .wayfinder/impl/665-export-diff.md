# 665 — 会话导出 diff 读面

**What to build:** SessionExportDiff.between（标量/消息三桶/state/extensions 有界差异）+ DiffReport record + 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SessionExportDiff + FieldDiff/MessageDiff/StateDiff/DiffReport
- [x] ExportDiffTest（identical/标量/消息三桶/state/跨会话 fail-fast/封顶）
- [x] spec 912 + README 行（欠账累计 906–912 七行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=ExportDiffTest` 全绿。commit 见本轮 `feat(core)` 提交。
