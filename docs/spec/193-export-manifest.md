# Spec 193 — 导出防篡改清单（effort #215）

> wayfinder map：`.wayfinder215/MAP.md`（T565–T566）。借鉴：OCI manifest /
> TUF——分发物带摘要清单，接收方可验完整性。

## Problem Statement

会话导出 JSON（spec 28）/gzip（spec 119）作为工单附件或取证材料搬运后，
接收方无法证明文件未被篡改/替换：合规场景（审计/取证）需要「导出时的内容」
与「收到的内容」一致的密码学凭证。

## Solution

`ExportManifest`（core/session，纯函数）：

- **构建**：`add(sessionId, contentJson)` 逐项 sha256(strip(content))；重复
  add 同 id 覆盖。
- **清单**：`manifestJson()` —— JSON：`{entries:[{id,digest}...按 id 序],
  totalDigest}`（totalDigest 对全表归一串再哈希——一票总凭证）。
- **校验**：`verify(manifestJson, Map<id, content>)` → `Verification(ok,
  mismatchedIds, missingIds, unexpectedIds)`——逐项核对 + 缺失/多出列出。

## User Stories

1. 作为审计员，导出包附 manifest——接收后 verify 一行确认完整性，篡改/
   缺失逐 id 指认。
2. 作为宿主，totalDigest 一条串即整批凭证（工单系统存一条即够）。
3. 作为取证，任何一字节差异都体现在 mismatch 列表——证据链完整。

## Implementation Decisions

- digest = sha256(strip(content)) hex；totalDigest = sha256(id=digest;…id 序)。
- manifestJson/verify 各自独立可存取（manifest 本身是普通 JSON 字符串）。

## Testing Decimals

- 摘要稳定（同内容双算一致、顺序无关）；篡改检出（单字节改）；缺失与多出
  列出；空清单边界；verify 对手工构造 manifest 的解析容错。

## Out of Scope

- HMAC 签名（归 webhook 签名族）；加密；自动附加导出口。

## Further Notes

- 导出完整性链：导出（28/119）→ gzip（119）→ 防篡改清单（本轮）。
