# Spec 194 — manifestGzip + resetAll（effort #141）

> wayfinder map：`.wayfinder141/MAP.md`（T556–T557）。组合小轮。

## Solution

①`exportManifestGzip(OutputStream)`：清单面接入 gzip 族管线（spec 109 同款）
——解压与明文逐字节一致，跨环境搬运清单随数据体同降。②`VirtualKeys
.resetAll()`：整窗换窗——全部 key 用量与耗尽态同清、限额表保留（窗口内
照常扣减）。

## Testing Decisions

- 红队：gzip round-trip 逐字节一致；resetAll 清耗尽/用量、留限额。
