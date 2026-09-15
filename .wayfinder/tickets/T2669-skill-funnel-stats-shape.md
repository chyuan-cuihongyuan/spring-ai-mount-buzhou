---
id: T2669
title: 技能漏斗读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

SkillFunnelStats 的形状怎么裁决？（spec 1734 / effort #1734 / R35）（spec 1734 验收/裁决）

## Resolution

实例面 recordSearch/recordLoad/recordApply 三计数+census(loadRate/applyRate 分母 0 哨兵 −1)——PostHog 漏斗思想：搜索没人加载=排序/描述问题，加载没人应用=内容问题。
