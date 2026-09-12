# 472 — spill 预览头尾语义

**What to build:** RangeReadEngine.previewOf 截断路径改头 3/4 + 省略标注 + 尾 1/4。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 头尾预览 + 常量抽取（3/4 头占比）
- [x] 3 用例绿 + spill 全模块 126/126 零回归
- [x] spec 619 + README 行

## Done

验证：`mvn -pl buzhou-spill -am test` 绿。commit 见本轮 `feat(spill)` 提交。
