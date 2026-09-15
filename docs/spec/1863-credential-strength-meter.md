# Spec 1863 — 凭据强度计（effort #1863，R64）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2927–T2928，impl 1464）。借鉴：
> 密码强度计惯例（NIST SP 800-63 长度优先 + 字符类别多样性）——评估
> 已知凭据的弱度，与秘密扫描（检测文本）互补：扫描器防泄漏、强度计
> 防弱钥。

## Problem Statement

配置/测试里的凭据（API key、默认口令、生成的令牌）只有「有没有」检查
 没有「弱不弱」评估：弱钥（短、单一字符类）静默落盘——被爆破的根因
 在落盘前不显形。

## Solution

`CredentialStrengthMeter`（buzhou-guard/secret，静态纯函数）：

- `charClasses(credential)`：小写/大写/数字/符号四类计数（0–4）；
- `band(credential)` 双条件阶梯：类别 ≥ 3 且长度 ≥ 16 → STRONG；类别
  ≥ 3 且长度 ≥ 12 → FAIR；否则 WEAK（常量 12/16/3 显式）；
- 凭据空白 fail-fast。

## User Stories

1. 作为配置校验作者，测试 API key 落 WEAK 档即告警换钥——弱钥在落盘
  前显形。
2. 作为安全审计者，四类计数+NIST 长度阶梯——简单口径可解释可复核
 （诚实边界：类别多样性是必要非充分，深度评分归 zxcvbn 类）。
3. 作为框架宿主，凭据口径自声明，纯评估不拦截。

## Implementation Decisions

- 纯评估；与 SecretScanner 的熵检测口径显式分界（文本检测 vs 已知凭据
  评分）。

## Testing Decisions

- 类别计数四例；双条件阶梯四例（短杂/单类/12 位/16 位）；边界含上
 （12→FAIR、16→STRONG）；空白 fail-fast。首跑编译红（枚举返回值误用
  hasSize）拆分断言修正。

## Out of Scope

- 不做字典/模式检测（zxcvbn 深度评分归未来静脉）；不拦截落盘。

## Further Notes

- 与 SecretScanner 互补成「防泄漏+防弱钥」对。
