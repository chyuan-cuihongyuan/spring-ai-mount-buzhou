# 826 — 注入检测分级策略

> 来源：H 会话第 27 轮 = effort #826 / [T1153](../../.wayfinder/tickets/T1153-injection-paranoia-policy.md) / [T1154](../../.wayfinder/tickets/T1154-injection-paranoia-policy-verify.md) / impl 579。
> 借鉴：ModSecurity paranoia levels（WAF 敏感度分级通行思想）。

## Problem

注入检测的连续分数没有分级裁决：0.72 分在「放行/记录/拦截」间如何取舍取决于场景风险偏好——一刀切阈值缺「级高更严、级低少扰」的档位语义。

## Solution

`InjectionParanoiaPolicy`（guard.classifier，纯函数）：

- **四级阈值**：L1=0.95（只拦几乎确定）→ L4=0.50（激进/红队演练）。
- **三态裁决**：BLOCK（≥阈值）/ LOG（观察带 [阈值−0.10, 阈值)）/ ALLOW。
- **防御**：分数截断 [0,1]；边界 ≥ 含等号；null fail-fast。

## 兼容性

纯新增（InjectionClassifier 零变更——verdict 喂 decide 即可）。

## 诚实边界

阈值固定标准档（不可配——简化口径）；LOG=记录不拦（征询归 hook）；分级不改变检测规则集（与 ModSecurity「级高激活更多规则」的机制差异诚实声明——本类只做分数→裁决映射）。
