---
id: T1128
title: 技能发布通道解析验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1127]
created: 2026-09-13
---

## Question

版本比较语义/回退链/归一口径如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 14 轮 = effort #813）：SkillChannelResolverTest 5 例——三通道显式解析/未标定回退/0.10.0>0.9.0+短段补 0+0-beta<0+latest 缺失取最高/同通道收敛/空通道归一+五种脏形态+unknown 空。首跑抓获 prerelease 排序反转与 List.of 拒 null 两处。
