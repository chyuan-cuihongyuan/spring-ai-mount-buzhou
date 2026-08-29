# Wayfinder Map — Buzhou per-tool 会话配额（effort #211，B 会话第 34 轮）

> B 会话第 34 轮。配额面观察：日配额（spec 16）限会话<b>总量</b>（turns/
> tool-calls/tokens），但「单工具爆用」（模型循环调某个贵工具 50 次）要等总量
> 耗尽才被拦。借鉴云厂商 per-API 配额（每接口独立额度）。

## Destination

ToolQuotaHook（guard/hook，order 250）：per-tool 会话内调用上限（Map 工具名→
上限 + "*" 通配默认）；beforeTool 计数于会话态（buzhou.tool-quota.<tool>），
超限 block 可读理由；per-session 自然隔离（会话态生命周期）；计 blocked。

## Notes

- 号段：B=奇数 spec（本轮 185）；轮次 .wayfinder200+。
- 与日配额（16，全局日窗）正交：这是会话内单工具面。
- 未配置工具 "*" 未配 = 零限制零变化。

## Decisions so far

- 计的是「执行尝试」（beforeTool 放行即计）——被拒不计。

## Not yet specified

- 重置 API；配额消费事件外发。

## Out of scope

- 沿用各轮；跨会话共享配额；token 加权配额。

## Tickets

- [x] [T557 ToolQuotaHook（per-tool 会话内上限）](tickets/T557-tool-quota.md)（impl-306）
- [x] [T558 配额回归（超限拦/通配/隔离/自然重置）](tickets/T558-tool-quota-tests.md)（impl-306）
