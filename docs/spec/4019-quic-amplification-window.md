# Spec 4019 — QUIC 反放大窗（effort #4019，R20）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6039–T6040，impl 2120）。
> 借鉴：QUIC RFC 9000 §8.1 地址验证防放大。

## Problem Statement

未验证对端（IP 伪造源）可诱骗服务端当反射器放大攻击第三方——
「发送量 ≤ 接收量 × k」的临时闸件缺失。

## Solution

`QuicAmplificationWindow`（core/backpressure，纯逻辑）：

- 信用窗：onReceived 入账 ×factor（默认 3）、onSent 扣减、超信用
  fail-fast（MUST NOT 放大语义）；
- 初始授信覆盖握手首包（未收包也要能回）；validateAddress
  解除窗（此后全速、credit 读 Long.MAX_VALUE 语义面）；
- canSend/credit/validated 三读数。

## User Stories

1. 作为接入作者，未验证源的响应量有硬顶——反射放大面关闭。
2. 作为协议作者，地址验证通过即恢复全速——窗是临时的。

## Testing Decisions

- 千字节收包 → 3 倍恰界（3000 可/3001 不可）+ 2999 扣减；
  两笔累积 450/200 扣减；超发 IllegalStateException + 恰尽后再发
  拒；验证解除全速 + credit MAX 语义；初始授信 1200 覆盖握手 +
  畸形六型 fail-fast。

## Out of Scope

- 不做地址验证协议本身（RETIRE/NEW_TOKEN 归协议层）；不做
  per-peer 多窗注册表；不做字节之外的包计数口径。

## Further Notes

- 与 HierarchicalTokenBucket（稳态限速）互补：本件是未验证对端
  的临时放大闸。
- 里程碑：20/50。
