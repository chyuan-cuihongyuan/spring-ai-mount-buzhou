# 1509 — design-incompleteness 小缺口清扫（F7 + F10）

> 来源：M 会话第 10 轮 = effort #1509（impl 1112）。评审清单 F 系缺口的清扫轮（S1/S2 已于 spec 1507/1508 闭环）。

## 背景

- F7：`canary.selected` 事件 payload 只有 model/primary，缺 sessionId——常量 Javadoc 自钉「sessionId + model」未兑现，多会话共用监听面无法定位归属。
- F10：spec 07 以推演名 `AgentSession.resume()` 描述续跑，实现定名为 `SessionInterrupts.resumeWith`（spec 12 面）——文档未回写。

## 目标

- F7：payload 补 sessionId（null 会话上下文省略——Map.of 不容 null，条件包含后 copyOf）；
- F10：spec 07 两处回写指向 resumeWith（功能等价、名不同）。

## 兼容性

事件面新增字段（监听方按 key 取，兼容）；文档回写零行为变化。
