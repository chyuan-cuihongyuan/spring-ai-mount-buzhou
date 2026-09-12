---
id: T882
title: 技能目录清单指纹的契约面裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

工具目录有 SHA-256 指纹与漂移对账（spec 175/201）；技能目录（yml/DB 动态源）无对应面——目录漂移（技能被改描述/改权限/增删）不可对账。技能的「契约面」是什么？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 17 轮 = effort #600 / spec 616 / impl 469）：

1. 契约面 = name + description + allowedTools：description 是模型选技能的依据（契约非文档——与 175 的「description 不计」差异诚实入档）；allowedTools 是权限面必须显形。
2. `SkillCatalogFingerprint.of(catalog)`：per 技能 sha256(description|allowedTools) + 整体摘要；输入序不敏感；`diff` 三分类（ADDED/REMOVED/CHANGED）。
3. 先原语后接线（175→201 先例）：漂移看门狗接线留雾区。
