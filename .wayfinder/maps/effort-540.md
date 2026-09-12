# Wayfinder Map — Buzhou 签名双密钥轮换验签（effort #540，E 会话第 40 轮）

> E 会话第 40 轮（428 验签扩散轮）。勘察：428 verify 单密钥——密钥轮换
> 窗口内旧签名全部验不过（消费端被迫与生产端同步原子换钥）。Stripe
> 轮换窗口思想：新旧密钥并存验签。

## Destination

WebhookSignatures.verifyWithRotation：先 current 后 previous（previous
可 null 单密钥期）——轮换窗口内旧签名可验；×容差窗组合重载（轮换 ×
重放窗）；fail-closed 语义不变。

## Notes

- 号段：spec 540 / T833–834 / impl-441。
- 借鉴源：Stripe multiple webhook signing secrets（轮换并存）。

## Out of scope

- 三密钥以上环形；自动轮换调度。

## Tickets

- [x] [T833 轮换验签](../tickets/T833-signature-rotation.md)
- [x] [T834 轮换×容差窗](../tickets/T834-rotation-tolerance.md)
