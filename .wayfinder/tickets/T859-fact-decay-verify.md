---
id: T859
title: 事实衰减验证口径（半衰边界/公式/直通/兼容）
type: task
status: closed
assignee: zcode-f
blocked-by: T858
created: 2026-09-12
---

## Question

衰减语义如何钉住不回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（DecayingFactStoreTest 5/5 + DefaultFactStoreTest 7/7 含新信封用例）：

- 半衰 4/下限 0.25：conf 0.4 第 4 轮滤、conf 1.0 第 8 轮恰 0.25（≥ 含）过、第 9 轮滤。
- 公式精确：2^(−4/4)=0.5、2^(−8/4)=0.25；elapsed 0 原值。
- save/delete 直通（衰减只影响注入读）；默认事实（1.0 + 长半衰）不惊扰。
- 信封：confidence 往返 0.6；手写旧信封（无该字段）读 1.0。
- 校验：半衰非正 / floor≥1 / null delegate / confidence 1.5 构造拒绝。
