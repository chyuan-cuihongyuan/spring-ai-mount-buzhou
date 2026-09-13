# 848 — 配置默认偏离审计

> 来源：H 会话第 49 轮 = effort #848 / [T1197](../../.wayfinder/tickets/T1197-config-deviation-audit.md) / [T1198](../../.wayfinder/tickets/T1198-config-deviation-audit-verify.md) / impl 600 续。
> 借鉴：Spring Boot configuration metadata（≈78K star）。

## Problem

「这套环境改了哪些默认配置」是环境漂移的最小画像：ConfigDiff 是快照间对比——vs 出厂默认的偏离清单缺位（排障时想看「非默认项」）。

## Solution

`ConfigDeviationAudit`（core.config，纯函数）：

- **对账**：audit(currents, defaults)——String.equals 比较；默认缺失键=无基线不裁决（不计 configured）；偏离清单典序封顶 32（key/currentValue/defaultValue）。
- **报告**：configuredKeys/deviatingKeys/deviationRatio。

## 兼容性

纯新增静态工具（默认值来源不绑定——调用方采集）。

## 诚实边界

字符串相等比较；无基线不裁决；空值键跳过。
