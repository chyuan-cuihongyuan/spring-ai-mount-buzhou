---
id: T930
title: fork 谱系 state 键公共常量化的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-13
---

## Question

fork 谱系三个 state 键字符串（`buzhou.fork.source` / `buzhou.fork.turn` / producer `buzhou.core.fork`）在写入口（DefaultAgentRuntime）与读入口（BuzhouSessionsEndpoint 面板、导出消费方、测试）各持一份字面量复制——复制会漂移，漂移 = 谱系断（写 A 读 B 计数永远 0）。收口吗？收在哪？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 41 轮 = effort #600 / spec 640 / impl 493）：收口为 `core.session` 公共常量类 `SessionForkKeys`（SOURCE / TURN / PRODUCER 三常量），写读两侧同源引用。放公共 API 包而非 internal——谱系键是 state 面对外契约（导出/导入跨环境携带，外部读面需要同一键名），值三处钉死不变（wire 契约）。借鉴 gRPC `Metadata.Key` / k8s apimachinery 常量类收口惯例。
