# Wayfinder Map — Buzhou 摘要槽信封加密·333 扩散（effort #336，C 会话第 37 轮）

> C 会话第 37 轮。333 把消息槽加密落地并明示 Out of scope：「SummaryStore/
> SessionStateStore 加密（同通道扩散另轮）」。本轮兑现前半：摘要（会话
> 长期记忆的浓缩面，PII 密度最高的数据之一）进同一信封通道。

## Destination

`EncryptingSummaryStore`（SummaryStore 装饰器：载体摘要——sections 单
结构键承载信封，sessionId/version/tokenEstimate/createdAt 明文路由，
AAD 绑定 sessionId+createdAt；latest/history/save 透明往返；旧明文摘要
透传兼容）+ BPP 扩散（BuzhouStores 重建时同时换 message+summary 两槽）
+ 诚实边界：SessionStateStore **不加密**——CAS 比值面
（deleteIfValueMatches/compareAndSwap 拿明文 expectedValue 比对底层值，
密文化即全线断裂）。

## Notes

- 号段：spec 336 / T663–T664 / impl-359。
- 借鉴源：同 333（Vault transit / KMS envelope）——本轮是扩散轮非新机制轮。
- 纪律：未配 master-key 零变化（BPP 单开关管两槽）；篡改宁可炸。

## Decisions so far

- 载体摘要保留 version 占位 0——真实版本由底层 save 的原子 UPSERT 分配
  （32 语义不破），解密还原时以底层回值为准。
- sections 单键 `__envelope__` 是结构标记非敏感内容；真 sections 全量进密文。

## Out of scope

- SessionStateStore 加密（CAS 比值面不兼容——诚实记录而非硬上）；
- 导出 JSONL 加密（导出族明文是宿主审计口径）。

## Tickets

- [x] [T663 EncryptingSummaryStore 载体往返](../tickets/T663-summary-encryption.md)
- [x] [T664 BPP 双槽扩散 + 收口](../tickets/T664-dual-slot-bpp.md)
