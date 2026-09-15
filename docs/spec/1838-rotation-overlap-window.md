# Spec 1838 — 密钥轮换重叠窗（effort #1838，R39）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2877–T2878，impl 1439）。借鉴：
> TLS 证书轮换 / Vault 密钥 grace——旧钥在重叠窗内仍有效（在飞数据兼容），
> 窗尽才真失效；无重叠窗的轮换=用旧钥数据瞬间全废。

## Problem Statement

凭据轮换只有「当前代」概念：切换瞬间旧代全废——用旧钥加密的在飞数据
解不开、旧签名验不过（蓝绿不接）；但永远双代有效又无收敛——「重叠多久、
哪些凭据还挂在窗内、清扫进度如何」没有判面。

## Solution

`RotationOverlapWindow`（core/crypto，静态纯函数）：

- `validity(tokenEpoch, currentEpoch, graceEpochs)` → 三态 `CURRENT /
  GRACE / EXPIRED`：同代 CURRENT；相差 ≤ graceEpochs 为 GRACE（含边界）；
  更旧 EXPIRED；**未来代 fail-fast**（tokenEpoch > currentEpoch——单调性
  违和，时钟/协议错不吞）；
- `census(currentEpoch, graceEpochs, epochs)` 普查（三态计数 +
  expiredRatio 清扫进度，无凭据 -1 哨兵）。

## User Stories

1. 作为密钥治理者，graceEpochs=2 → 两代重叠：切换后旧代数据还有两代
   的兼容窗口可解，蓝绿相接。
2. 作为清扫编排者，expiredRatio=0.2 且不再降 → 五分之一凭据挂窗未清，
   该追责归属。
3. 作为安全审计者，未来代凭据直接 fail-fast——时钟回拨或协议错不静默。

## Implementation Decisions

- 纯判态不执行（轮换归宿主）；epoch 单调性是硬约束（未来代拒绝）。
- 零宽窗合法（硬切换对照面——语义连续可调）。

## Testing Decisions

- 三态+边界含（lag==grace 为 GRACE）；零宽窗退化；普查+占比+哨兵；畸形
  四型（负 epoch/宽、未来代、null 凭据）fail-fast。

## Out of Scope

- 不执行轮换/清扫；不做窗宽自适应（在飞量驱动归未来静脉）。

## Further Notes

- 与 EnvelopeCipher 正交：那是加解密本体，这是轮换节奏判态。
