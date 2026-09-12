---
id: T971
title: 会话导出 unchanged 协商的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

周期性同步方（备份/下游流水线）反复拉全量会话导出——会话没变也要重收全量 payload。HTTP ETag / If-None-Match（304 语义）怎么映射？关键障碍：SessionExport JSON 含 exportedAtEpochMs（每次导出必变——现校验和随时间漂移，无法当 ETag）。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 11 轮 = effort #710 / spec 710 / impl 513）：① `SessionExportChecksum.contentFingerprint(SessionExport)`——对**内容投影**（sessionId/appId/agentName/messages/summary/state/extensions，显式剔除 exportedAtEpochMs）做 canonical JSON → sha256，前缀 `sha256-c:`（与既有整体校验和 `sha256:` 区分——防混用）；② `SessionExportConditional`（core/session）：`static Result exportIfChanged(fresh, ifNoneMatch)` → Result{status EXPORTED|UNCHANGED, export, contentFingerprint}——指纹相等 = UNCHANGED（payload 不外发，同步方跳过写入）；不等或 ifNoneMatch 空 = EXPORTED。匹配头异常形态 fail-open 走 EXPORTED（协商失败不给 304）。借鉴 HTTP ETag / If-None-Match / RFC 7232 条件请求。
