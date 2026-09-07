# Wayfinder Map — Buzhou 导出防篡改清单（effort #215，B 会话第 38 轮）

> B 会话第 38 轮。会话导出（spec 28/gzip 族）搬运时无完整性凭证——接收方无法
> 证明「收到的就是导出的」。借鉴 OCI manifest / TUF（分发物带摘要清单）。

## Destination

ExportManifest（core/session）：逐会话 sha256 摘要 + 总摘要的 manifest
（JSON）；verify 逐项核对并列出 mismatch/missing——合规搬运（工单附件/取证）
的防篡改凭证。

## Notes

- 号段：B=奇数 spec（本轮 193）；轮次 .wayfinder200+。
- 纯函数（digest/manifest/verify 三步）——不绑定导出管线（装配侧组合）。

## Decisions so far

- 摘要按 strip 后内容算（与全仓哈希纪律一致）。

## Not yet specified

- 与 exportSession/exportSessionGzip 出口自动附加。

## Out of scope

- 沿用各轮；签名（HMAC 归 webhook 签名族）；加密。

## Tickets

- [x] [T565 ExportManifest（逐项+总摘要+verify）](../tickets/T565-manifest.md)（impl-310）
- [x] [T566 清单回归（摘要稳定/篡改检出/缺失列出/空清单）](../tickets/T566-manifest-tests.md)（impl-310）
