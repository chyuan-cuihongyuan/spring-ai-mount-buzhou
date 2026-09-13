# effort #807 — 签名密钥轮换到期审计

- 会话：H 会话 800 系第 8 轮 ｜ spec [807](../../../docs/spec/807-key-rotation-audit.md) ｜ 票 [T1115](../tickets/T1115-key-rotation-audit.md)/[T1116](../tickets/T1116-key-rotation-audit-verify.md) ｜ impl560
- 借鉴：cert-manager（cert-manager/cert-manager ≈13K star）——证书到期监控/renewal 提前量思想

## 勘察（排重）

- SigningKeyRing（impl-39/spec 41）：版本制（rotate/minVerifyVersion）——无时间维。
- AuthTtl：认证 TTL（token 时效）非签名钥龄。
- grep -i `notAfter|expiry|renew`：无命中——密钥龄族缺位。

## 决定

`KeyRotationAudit`（guard.audit，纯函数）：audit(activationTimes, activeVersion, hasSigningKey, now, maxAge, warnBefore)——OVERDUE(age≥maxAge)/DUE_SOON(age≥maxAge−warnBefore)/OK 三档 + UNKNOWN_ACTIVE（active 版本无激活账——账本落后异常面）；最坏排序（OVERDUE→UNKNOWN_ACTIVE→DUE_SOON→OK，同级 age 降序）；脏账跳过；warnBefore>maxAge 等 fail-fast。SigningKeyRing 零侵入（时间维由 persister 记账——环保持版本制语义）。

## 测试

三档边界精确（恰达 maxAge/恰达临期线/临期窗内外）/最坏排序含同级 age 降序/UNKNOWN_ACTIVE 置顶/无钥降级如实/脏账跳过/四种 fail-fast——6 例全绿（首跑边界预期笔误：age=999 在临期窗内应为 DUE_SOON——实现与 spec 一致，修测试）。

## 诚实边界

激活时刻由调用方记账（环本身无时钟——零侵入取舍）；纯读数不触发轮换（rotate 是环的写面）；grace 过期钥验证语义归 minVerifyVersion（本类只报不裁）。
