# impl 1590 — 版本要求判定（spec 2039 / T3181–T3182 / R40）

纵切片：`VersionRequirement`（buzhou-skills 主）+
`VersionRequirementTest`（九用例）。六算子、0.x 锁定、prerelease、
短补 0。

- 测试：`mvn -pl buzhou-skills test -Dtest=VersionRequirementTest` 9/9 绿。
- 教训入档：纯函数类不得用实例字段传递中间态（lastVersion hack）——
  解析一次、参数传递。
