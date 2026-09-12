# 720 — 错误签名已知问题静默标记

> 来源：G 会话第 21 轮 = effort #720（借鉴 getsentry/sentry muted/resolved issues）/ [T991](../../.wayfinder/tickets/T991-signature-mute-shape.md) / [T992](../../.wayfinder/tickets/T992-signature-mute-verify.md) / impl 523。

## 背景

ErrorSignatures（spec 44/83）top-N 被已知问题（上游依赖抖动、已立案缺陷、待升级组件）长期霸榜——新错误族被挤出视野，告警疲劳。Sentry 的洞察：已知问题可静默（muted）——退出收件箱但不丢事实。

## 目标

- `ErrorSignatures` 增静默标记面：
  - `mute(String sig)`（有界 MUTED_CAP=64，超限拒 false）/ `unmute(String sig)` / `mutedSignatures()` 不可变视图；
  - `top(n)` / `top(kind, n)` 排除 muted（读面降噪）；
  - **计数照常累计**——`snapshot()` 原样含 muted（原始事实可查：静默是读面降噪不是数据删除）；
  - `reset()` 连带清空 muted（新纪元语义）。

## 非目标

不做静默窗口期/TTL（永久静默直到 unmute/reset——时间窗语义留位）；不做静默原因标注（Sentry assignee/注释面留位）。

## 测试

mute 后 top 消失/snapshot 照常、unmute 回归、cap 拒绝、reset 清空、kind 分面同样排除、视图不可变。

## 兼容性

additive 方法；top* 默认行为零变化（无人 mute 时排除集为空）。
