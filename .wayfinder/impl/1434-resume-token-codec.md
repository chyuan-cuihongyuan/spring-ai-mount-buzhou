# impl 1434 — ResumeTokenCodec 续读令牌（R34 = effort #1833 / spec 1833 / T2867-T2868）

**What**：`ResumeTokenCodec`（core/session 静态纯函数）——ResumeToken 契约
构造 + encode/decode 回路（指纹@偏移）+ check 三态（指纹先行：VALID/
STALE_DATA/OUT_OF_RANGE，offset==max 为读到尾）；畸形令牌四型与入参三型
fail-fast。

**Why**：continuation token/ETag 思想——游标只带偏移不带指纹则数据换代后
静默跳号；STALE_DATA（重拉首页）与 OUT_OF_RANGE（查保留策略）分诊重试，
换代率即游标失效频率读数。

**Verify**：`ResumeTokenCodecTest` 4 用例全绿。

**Status**：done（2026-09-16）
