---
id: T908
title: 技能漂移看门狗的巡查宿主裁决（渲染节拍）
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

SkillCatalogDriftWatcher（spec 617）无调度——谁来触发 check？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 30 轮 = effort #600 / spec 629 / impl 482）：

1. **渲染节拍即宿主**：清单每轮注入渲染（热点路径）顺带 watcher.check——零调度线程零新轮询，漂移最迟下一轮显形。
2. SkillCatalogRendererImpl 五参构造（watcher null = 零变化）；renderEntries 入口 check（缓存命中与否都查——指纹独立于渲染缓存）。
3. 装配：BuzhouSkillsAutoConfiguration 建 watcher（emitter=WARN 日志）经 SkillModule.catalogRendererWithDriftWatcher 供 bean。
