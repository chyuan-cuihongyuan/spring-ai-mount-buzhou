# 710 — 会话导出 unchanged 协商

> 来源：G 会话第 11 轮 = effort #710（借鉴 HTTP ETag / If-None-Match / RFC 7232 条件请求）/ [T971](../../.wayfinder/tickets/T971-export-etag-shape.md) / [T972](../../.wayfinder/tickets/T972-export-etag-verify.md) / impl 513。

## 背景

周期性同步方（备份、下游流水线、多实例巡检）反复拉会话导出——会话未变也要重收/重写全量 payload。关键障碍：SessionExport JSON 含 `exportedAtEpochMs`（每次导出必变），既有整体校验和（spec 547）随时间漂移，无法当 ETag 用。

## 目标

- `SessionExportChecksum.contentFingerprint(SessionExport)`：对**内容投影**（sessionId/appId/agentName/messages/summary/state/extensions——显式剔除 exportedAtEpochMs）做 canonical JSON → sha256，前缀 `sha256-c:`（与既有整体校验和 `sha256:` 显式区分，防混用）。
- `SessionExportConditional.exportIfChanged(fresh, ifNoneMatch)`：指纹相等 → `UNCHANGED`（result.export 为 null——payload 不外发，同步方跳过写入）；不等或空 → `EXPORTED`（携 export + 新指纹，同步方持久化指纹供下轮协商）。匹配值异常形态 fail-open 走 EXPORTED（协商失败不给 304）。

## 非目标

不改既有 `of(toJson())` 整体校验和语义（511 防衰变 doctrine 不动）；不做 HTTP 层面（纯库内协商——服务端映射留宿主）。

## 测试

同内容异时戳 → UNCHANGED；任一内容字段变化 → EXPORTED + 新指纹；垃圾匹配值 fail-open；内容指纹 ≠ 整体校验和；同调同指纹稳定。

## 兼容性

纯增量；既有校验和语义零变化。
