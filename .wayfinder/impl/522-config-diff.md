# 522 — 生效配置 diff 读面

**What to build:** ConfigDiff.diff(before, after)——ADDED/REMOVED/CHANGED 三分类 + 字典序稳定 + 掩码同值语义 + 不可变输出。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ConfigDiff（Entry/Kind + 纯函数 diff）
- [x] 三分类/字典序/掩码/null/不可变用例
- [x] spec 719 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
