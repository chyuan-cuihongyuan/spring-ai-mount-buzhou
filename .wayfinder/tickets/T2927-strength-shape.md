---
id: T2927
title: 凭据强度计的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

已知凭据「弱不弱」怎么评估？（spec 1863 / effort #1863 / R64）

## Resolution`

**密码强度计惯例（NIST SP 800-63 长度优先+字符类别多样性）纯评估
`CredentialStrengthMeter`（buzhou-guard/secret）**：charClasses 四类计数
（小写/大写/数字/符号）+ band 双条件阶梯（类别≥3 且长度≥16→STRONG；
类别≥3 且长度≥12→FAIR；否则 WEAK——常量 12/16/3 显式）；空白凭据
fail-fast。与 SecretScanner（文本检测防泄漏）互补成防弱钥面。诚实边界：
类别多样性必要非充分，深度评分归 zxcvbn 类。

