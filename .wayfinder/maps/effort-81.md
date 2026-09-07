# Wayfinder Map — Buzhou 会话级 gzip 导出（effort #81，50 轮自迭代第 46 轮）

> effort #81，延续 #80（T425–T426 / impl-265）。主线：spec 109 gzip 面只有全量/
> 增量——单会话归档（工单附件/事故单会话取证）仍明文。

## Destination

`ObservabilityJsonlExporter.exportSessionGzip(sessionId, OutputStream)`：GZIP+
UTF-8 Writer 复用 exportSession 管线——解压与明文逐字节一致。

## Notes

- 借鉴：spec 109 同管线（单会话粒度补齐——gzip 三入口族完整）。

## Decisions so far

- 同管线复用（一致性由测试钉住）。

## Not yet specified

- eval/AB 导出 gzip（量级小——需求后议）。

## Out of scope

- 沿用 #7–#80。

## Tickets

- [x] [T427 exportSessionGzip 会话级入口](../tickets/T429-session-gzip.md)（impl-266）
- [x] [T428 红队（与明文逐字节一致）+ 收口](../tickets/T430-session-gzip-close.md)
