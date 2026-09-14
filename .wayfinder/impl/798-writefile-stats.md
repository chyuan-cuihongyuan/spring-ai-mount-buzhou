# 798 — write_file 写入量水位与拒绝分桶读面

**What to build:** WriteFileTool 静态七计数（attempts/writes/bytesWritten/paramRejects/oversizeRejects/noclobberRejects/failures）+ 嵌套 WriteFileStats + stats()/resetForTest() + 成功/缺参/超限/noclobber/沙箱逃逸/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 七计数落点（入口/成功/三显式拒绝/catch 兜底；bytesWritten=成功写入 UTF-8 字节）
- [x] WriteFileStats 嵌套 record + stats() + resetForTest()
- [x] WriteFileStatsTest（TempDir 沙箱：成功/缺参/超限/noclobber/逃逸/守恒/reset 七测）
- [x] spec 1046 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-tools -am test -Dtest='WriteFileStatsTest'` 全绿 + 既有 WriteFileTool 回归绿。commit 见本轮 `feat(tools)` 提交。
