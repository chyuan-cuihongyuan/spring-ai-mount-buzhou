# Spec 540 — 签名双密钥轮换验签（effort #540）

> wayfinder map：`.wayfinder/maps/effort-540.md`（T833–834）。E 会话第 40 轮。

## Problem Statement

428 verify 单密钥——密钥轮换窗口内旧签名全部验不过（消费端被迫与生产端
同步原子换钥）。Stripe 支持多签名密钥并存（轮换窗口）。

## Solution

WebhookSignatures.verifyWithRotation 两个重载：

- (current, previous, body, sig)：先 current 后 previous（previous 可
  null=单密钥期）——轮换窗口内旧签名可验。
- 轮换 × 容差窗组合：(current, previous, body, sig, timestamp,
  tolerance)——任一密钥验签过后再过重放窗。
- fail-closed 语义不变（null/空/畸形 false）。

## User Stories

1. 作为消费端，我想轮换密钥时旧签名仍可验， so 生产/消费端不必原子
   同步换钥（轮换窗口平滑过渡）。

## Implementation Decisions

- 双密钥（current+previous）——Stripe 同款轮换窗口；三密钥环形不预设。
- previous blank = 无旧密钥（fail-closed 同 null）。

## Testing Decisions

- 新旧签名各自可验；未知密钥 false；×容差窗组合（新鲜过/过期不过/
  非数字 fail-closed）；null/blank fail-closed。

## Out of Scope

- 三密钥环形；自动轮换调度。

## Further Notes

- 无新顶层公共类型（静态方法加法）——快照零 diff 预期。
