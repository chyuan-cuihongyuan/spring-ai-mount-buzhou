# 1063 — 工具入参校验读数

**What to build:** ToolArgsValidator 静态读数面（validations/accepted+七错误桶标记单源+守恒）+ ValidationStats+reset + 六测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] MARK_* 常量单源化（check() 出错文本与分桶共用）
- [x] validate() 内计数（无可校验结构不入账）
- [x] ToolArgsValidationStatsTest 六测
- [x] spec 1410 + README 行（既有类静态字段——快照面不变）+ api-surface.md L 段

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='ToolArgsValidationStatsTest,ToolArgsValidatorTest'` 12/12 绿。
