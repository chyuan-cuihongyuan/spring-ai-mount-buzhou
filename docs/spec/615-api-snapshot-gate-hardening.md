# 615 — API 快照门硬化

> 来源：F 会话第 16 轮 = effort #600 / [T880](../../.wayfinder/tickets/T880-snapshot-gate-shape.md) / [T881](../../.wayfinder/tickets/T881-snapshot-gate-verify.md) / impl 468。思想：测试的副作用会吃掉它自己的断言（flaky-test 治理惯例——维护操作与断言路径分离）。

## 背景

ApiSurfaceSnapshotTest（T215/impl-179）声称「新增/移除公开类型未更新快照即失败」，但 regenerateSnapshot() 是普通 @Test 随套件执行并覆写快照文件——比对因此恒自愈，门从未真正拦截过漂移。

## 目标

regenerate 门控为显式维护操作（系统属性 `buzhou.api-snapshot.regenerate=true`）；常规 verify 只比对。

## 非目标

- 不改比对语义与扫描口径。

## 设计

@EnabledIfSystemProperty 门控；更新流程文档同步；显式再生补录 F 会话 5–15 轮 7 个新公共型入档。

## 测试

starter 套件 11 绿 + skipped=1（regenerate 不再自行执行）；显式属性下再生成功。

## 兼容性

快照文件内容与真实公共面全等（比对绿）；维护命令多一个 -D 参数（入档）。
