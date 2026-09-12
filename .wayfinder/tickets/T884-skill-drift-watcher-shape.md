---
id: T884
title: 技能目录漂移看门狗的形态裁决（201 镜像 + diff 方向修正）
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

impl 469 的指纹原语如何接上看门狗（175→201 先例）？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 18 轮 = effort #600 / spec 617 / impl 470）：

1. `SkillCatalogDriftWatcher` 镜像 201：首拍建基线不发事件（装配期变化是常态）、后续 check 漂移即 `skill.catalog.drifted` 事件（三分类+新旧摘要）+ 基线推进 + `buzhou.skill.catalog.drifted` 计数；无调度（宿主定时/事件触发）。
2. **接线时发现并修正 469 的 diff 方向缺陷**：SkillCatalogFingerprint.diff 原 added=in-this（与 175 的 added=参数侧相反）——已对齐 175 语义（`基线.diff(现)` 即时间正向），469 测试同步翻正。
3. 空 emitter 安全；null 清单按空目录。
