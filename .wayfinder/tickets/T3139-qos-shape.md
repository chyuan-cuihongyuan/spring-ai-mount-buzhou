---
id: T3139
title: QoS 资源声明分级的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

会话/租户资源声明的保护分级怎么定？（spec 2019 / effort #2019 / R20）

## Resolution

**K8s QoS Classes 纯函数分级 `QosClassifier`（core/policy）**：
ResourceRequest record（request 下限/limit 上限，矛盾负数 compact
守约）+ classify 三态（全维保额 G / 任一拉低 B / 全零或未声明 BE）+
evictionRank 驱逐序（BE 先让位 G 受保护）+ shouldYield 保护线让位判定
——驱逐与保护由分级驱动不再逐实例拍脑袋。
