---
id: T2275
title: 配置全键表 config-reference（F9 闭环）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 14 轮：spec 21:9 承诺的 docs/config-reference 全键表如何交付？

## Resolution

**用户常设授权 AFK（可推翻）**

形状：三段式生成文档——① @ConfigurationProperties record 组件全表（扫描 57 record / 198 组件键：前缀/键/类型/模块）；② fromYml(Map) 契约子键段（4 模块键清单 + 语义详注指针到模块 spec）；③ Environment 直读键（34 个 getProperty 字面量全列）。camelCase≡kebab-case relaxed binding 说明；组件级默认值指针 compact constructor；组件级中文语义注记诚实声明为后续增量轮（不做一次性假装完备）。
