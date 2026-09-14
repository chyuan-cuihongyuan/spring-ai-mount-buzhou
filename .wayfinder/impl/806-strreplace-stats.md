# 806 — str_replace 编辑判定读面

**What to build:** StrReplaceTool 静态七计数（attempts/successes + param/missingFile/notFound/ambiguous/failures 五拒绝桶）+ 嵌套 StrReplaceStats + stats()/resetForTest() + 六路径/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 七计数落点（入口/成功/两参数点合桶/文件不存在/occurrences 0 与 &gt;1 分桶/catch 兜底）
- [x] StrReplaceStats 嵌套 record + stats() + resetForTest()
- [x] StrReplaceStatsTest（成功/缺参/缺文件/未找到/歧义/守恒/reset 七测）
- [x] spec 1054 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='StrReplaceStatsTest'` 全绿 + 既有 StrReplaceTool 回归绿。commit 见本轮 `feat(spill)` 提交。
