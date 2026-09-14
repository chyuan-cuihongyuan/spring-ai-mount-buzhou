# 799 — read_file 读量水位与拒绝分桶读面

**What to build:** ReadFileTool 静态六计数（attempts/reads/bytesRead/notFileRejects/oversizeRejects/failures）+ 嵌套 ReadFileStats + stats()/resetForTest() + 成功/不存在/超限/逃逸/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 六计数落点（入口/成功/两显式拒绝/catch 兜底；bytesRead=成功读取 UTF-8 字节）
- [x] ReadFileStats 嵌套 record + stats() + resetForTest()
- [x] ReadFileStatsTest（TempDir 沙箱：成功/不存在/超限/逃逸/守恒/reset 六测）
- [x] spec 1047 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-tools -am test -Dtest='ReadFileStatsTest'` 全绿 + 既有 FileToolsTest/NoclobberTest 回归绿。commit 见本轮 `feat(tools)` 提交。
