# Wayfinder Map — Buzhou 会话隔离检疫（effort #100，B 会话第 12 轮）

> B 会话第 12 轮。主题池「会话隔离检疫」：单会话反复失败（坏上下文/毒输入循环）
> 会持续消耗模型调用——Erlang supervisor「let it crash + 退避重启」思想：
> 连败会话隔离冷却，指数退避逐级加长。

## Destination

SessionQuarantine（连败阈值跳闸→隔离冷却→指数退避升级→到时自动解除）+
SessionQuarantineHook（beforeTurn 准入 block / onModelError 计败）+
ErrorCode.SESSION_QUARANTINED。成功复位走公共 API（诚实边界：hook 面看不到
「健康轮」全貌，不谎装）。

## Notes

- 号段：B=奇数 spec（本轮 143）。
- 与 runaway（轮内步数预算）/ TurnHeartbeat（停滞观测）正交：这是会话级失败频次面。
- 不隔离=零变化；隔离只 block 不抛（模型/用户拿到可读理由）。

## Decisions so far

- 退避按跳闸次数指数升级（base×2^n 封顶 max）——毒会话越闹冷却越长。

## Not yet specified

- 检疫事件 webhook 外发；autoconfig 配置面。

## Out of scope

- 沿用 #7–#99；人工解禁 API；跨实例共享检疫状态。

## Tickets

- [x] [T495 SessionQuarantine 状态机 + ErrorCode](tickets/T495-quarantine.md)（impl-284）
- [x] [T496 检疫 hook 接线 + 回归](tickets/T496-quarantine-tests.md)（impl-284）
