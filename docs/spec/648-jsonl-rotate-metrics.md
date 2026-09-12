# 648 — JSONL 轮转事件指标化

> 来源：F 会话第 49 轮 = effort #600（spec 642 装配后补全——运维生效面）/ [T946](../../.wayfinder/tickets/T946-jsonl-rotate-metrics-shape.md) / [T947](../../.wayfinder/tickets/T947-jsonl-rotate-metrics-verify.md) / impl 501。

## 背景

spec 642 的 RollingJsonlWriter 只有编程 getter（rotations/rotationFailures）——yml 声明式部署（health timeline / shadow detail 导出的实际用户）无指标面：「磁盘保护是否真在工作（轮转发生过）、轮转路径是否有病灶（失败在累计）」运维不可见。

## 目标

- 轮转事件走 `BuzhouMetricsHolder` 全局面（库内默认 no-op 零开销；micrometer 装配后真实计数——impl-41 / spec 13 §T66 既有范式）：
  - 成功：`buzhou.jsonl.rotated`（tag `file` = 目标文件名，有界——文件数有限）；
  - 失败：`buzhou.jsonl.rotate-failed`（同 tag）。
- 长驻 rotate() 与静态 rotateIfNeeded 路径同发；编程 getter 保留（测试与编程消费方）。

## 非目标

不改轮转语义/阈值；不上健康面（指标即观测，健康段留给真正失能语义）。

## 测试

install 收集 metrics → 小阈值触发：rotated 计数与轮转次数一致（tag 命中）；静态路径同发；既有轮转用例零回归。

## 兼容性

纯增量指标事件；默认 no-op 下行为逐字节不变。
