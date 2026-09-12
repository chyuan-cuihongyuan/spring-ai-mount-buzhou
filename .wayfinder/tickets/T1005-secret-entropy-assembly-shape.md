---
id: T1005
title: 秘密熵过滤 Builder 装配的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

SecretScanner 熵阈值（spec 714）只有直接构造面——GuardModule 装配链（secretScanning 标志 → SecretScanHook）不可达。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 28 轮 = effort #727 / spec 727 / impl 530）：Builder.secretMinEntropy(Double)（null=关默认）；secretScanning 启用时统一构造 SecretScanner(types, minEntropy) 直通（types null=全类型与旧双分支等价）；SecretScanHook 增自带扫描器构造；GuardModule.hooksView() 包内观测面（装配测试断言缝——hooks 私有导致装配不可断言的测试盲区收口）。
