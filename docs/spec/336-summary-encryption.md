# Spec 336 — 摘要槽信封加密（effort #336）

> wayfinder map：`.wayfinder336/MAP.md`（T663–T664）。C 会话第 37 轮，
> 333 的扩散轮。

## Problem Statement

消息已静态加密（333），但会话摘要（StructuredSummary——长期记忆的
浓缩面）仍明文落库：sections 承载全部记忆内容，PII 密度不低于消息，
且摘要生命周期比消息更长（跨会话复用、进压缩/归档管线）。

## Solution

`EncryptingSummaryStore`（core.crypto，与 333 同通道同纪律）：

- **载体摘要**：save 时序列化真 StructuredSummary（sections 全量 +
  tokenEstimate + createdAt）→ `EnvelopeCipher` 加密 → 载体
  `StructuredSummary(sessionId, version=0 占位, {__envelope__: 信封},
  tokenEstimate, createdAt)`；version 真值由底层 save 的原子 UPSERT
  分配（32 并发语义不破），latest/history 解密还原时以底层版本为准。
- **AAD 绑定**：sessionId + createdAt——载体路由字段被篡改即解密失败。
- **透传兼容**：底层旧明文摘要（sections 无结构键）原样返回——迁移
  友好；已是信封不再包装（幂等）。
- **BPP 扩散**：`buzhou.security.message-encryption.master-key` 单开关
  同时换 message + summary 两槽（宿主零改动）；未配零变化。
- **诚实边界**：SessionStateStore 不加密——其 CAS 比值面
  （deleteIfValueMatches/compareAndSwap 以明文 expectedValue 比对底层
  值）与密文根本不兼容，硬上即全线断裂。

## User Stories

1. 作为安全负责人，我想摘要与消息同一把钥同一通道加密，所以 静态
   加密覆盖面无明文死角（除明示边界）。
2. 作为使用者，我想摘要版本号语义（UPSERT 原子递增）加密后不变，所以
   并发保存/压缩/时间旅行 fork 等既有行为零变化。
3. 作为使用者，我想旧明文摘要照常可读，所以 先升级后迁移可行。
4. 作为运维，我想一把 master-key 管两个槽，所以 轮换操作单点完成。
5. 作为审计者，我想 state 槽不加密的原因被明示，所以 边界是决策不是
   疏漏。

## Implementation Decisions

- 载体 sections 结构键名 `__envelope__`（结构标记，非敏感）。
- 序列化手写归一（sections/tokenEstimate/createdAt→epochMillis）——与
  333 同法不依赖 jackson-jsr310。
- BPP 一次重建换两槽（message + summary），其余四槽原样。

## Testing Decisions

- Store：载体形态（底层只见结构键+密文）/save-latest-history 全往返
  （版本以底层为准）/旧明文透传/幂等不重复包装/AAD 换绑失败。
- 装配：master-key 声明后两槽同时换装；未配两槽都原样。

## Out of Scope

- SessionStateStore 加密（CAS 比值面不兼容）；导出 JSONL 加密；
  归档冷层加密（归档物源自已加密的两槽时天然密文——宿主侧确认）。

## Further Notes

- 新公共类型 `EncryptingSummaryStore` 随轮 regenerate 快照。
