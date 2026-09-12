---
id: T938
title: JSON 行手工拼接的收口裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-13
---

## Question

spec 60 立了「绝不手工拼接、Jackson 序列化保证一行一记录」纪律，但仍有 4 处手工拼 JSON：PromptUsageJsonl（name 零转义——含引号/换行即畸形行）、ExportBundle.manifestJson（error 土法转义丢换行与信息）、FailureTurnSnapshots / WebhookDeadLetterJsonl（自有 escape 不含控制字符 <0x20——严格解析器拒收）。收口吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 45 轮 = effort #600 / spec 644 / impl 497）：四处统一走 Jackson `writeValueAsString`（LinkedHashMap 插入序 = 字段序稳定，与 spec 60 行形态纪律同构）。不造公共工具类——每处一个私有 MAPPER（或静态共享 ObjectMapper 常量，与 ObservabilityJsonlExporter 同模式）；对抗口径：name/error/preview 注入 `"` `\` 换行/制表符 → 行仍合法 JSON 且解析回读得原值。
