# 602 — 会话 fork 谱系（span-links 思想的关联面落地）

> 借鉴：[open-telemetry/opentelemetry-specification](https://github.com/open-telemetry/opentelemetry-specification) span links——因果相关但不构成父子关系的跨 trace 关联。
> 来源：F 会话第 3 轮 = effort #600 / [T854](../../.wayfinder/tickets/T854-fork-lineage-shape.md) / [T855](../../.wayfinder/tickets/T855-fork-lineage-verify.md) / impl 455。

## 背景

fork（spec 20）与时间旅行 fork（spec 311）复制历史开新分支，但子会话与源会话之间**没有任何持久关联**——排障时「这个分支从哪来」只能靠翻日志；guard/spill/dashboard 想按谱系做策略也无从查起。OTel 用 span links 表达「因果相关但非父子」；buzhou 的对应落点是会话 state 与事件（span 模型无谱系概念，诚实映射而非强行加伪 link）。

## 目标

1. fork / forkFromTurn 后，子会话 state 写入谱系条目：键 `buzhou.fork.source`、value=源会话 id、producer=`buzhou.core.fork`、无 TTL。
2. `session.forked` 事件携带 `copiedMessages`、`copiedSummary`（forkFromTurn 另有 `upToTurn`）。

## 非目标

- 不复制源 state（预算重置语义不变）。
- 不做 otel 侧真 span links（观测管线无 span 谱系模型——雾区另议）。
- 不做谱系链（fork 的 fork 追溯到根）——单级谱系够排障用，链式遍历留待需要时。

## 设计

- 写入点：spawn 完成、历史 append 之后（fork 监听器之前——监听器可读到谱系）。
- 导出（spec 28）state 段天然携带谱系；导入随之移植。
- 事件经既有 hook 链分发（fork 时点早于调用方注册 listener，谱系断言走 hook 捕获）。

## 测试

3 用例（见 [T855](../../.wayfinder/tickets/T855-fork-lineage-verify.md)）+ SessionForkEndToEndTest 零回归。

## 兼容性

纯增量：一条新 state 键 + 事件 payload 加字段；既有 fork 行为与 State 语义零变化。
