# Spec 1833 — 续读令牌编解码与裁决（effort #1833，R34）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2867–T2868，impl 1434）。借鉴：
> 分页 continuation token / 条件请求 ETag——令牌绑定签发时数据指纹，
> 续读=「从哪继续」×「还是那一版吗」。

## Problem Statement

游标分页只带偏移不带指纹：底层数据换代（重建/压缩/清理）后旧游标仍被
当有效——静默跳号或重读；「游标过期」与「游标越界」混为一谈，重试策略
无从分诊。

## Solution

`ResumeTokenCodec`（core/session，静态纯函数）：

- `ResumeToken(fingerprint, offset)` 契约构造 + `encode`/`decode` 回路
 （指纹@偏移；指纹侧容忍内含分隔符，按最后分隔符切）；
- `check(token, currentFingerprint, currentMax)` → 三态 `VALID / STALE_DATA
  / OUT_OF_RANGE`：**指纹先行**（换代优先于越界——数据都不是那版了，
  越不越界无意义）；越界边界：offset == max 为「读到尾」VALID。

## User Stories

1. 作为分页 API 使用者，STALE_DATA → 重新首页拉取（游标作废），OUT_OF_
   RANGE → 数据被裁剪、告警查保留策略——两种重试策略分开。
2. 作为数据治理者，指纹换代率即游标失效频率——频繁换代该查压缩/清理
   节奏。
3. 作为框架宿主，指纹口径（内容哈希/世代号）自声明，纯裁决零读取。

## Implementation Decisions

- 纯裁决不读取（数据访问归宿主）；换代优先于越界的裁决序显式入档。
- fail-fast：畸形令牌四型（null/无分隔/偏移非数/空偏移）+ 入参三型
 （负偏移/空白指纹/负水位）。

## Testing Decisions

- 编解码回路（含指纹内含分隔符）；三态+边界（== max VALID；换代+越界
  并存取 STALE）；畸形令牌四型；畸形入参三型 fail-fast。

## Out of Scope

- 不实现指纹计算（哈希归 EvalSetFingerprint 族）；不做不透明令牌加密。

## Further Notes

- 与 GapBackfillPlanner 配对：换代后先出缺口单再重读。
