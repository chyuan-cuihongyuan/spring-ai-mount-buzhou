# Spec 1874 — 正则风险审计（effort #1874，R75）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2949–T2950，impl 1475）。借鉴：
> OWASP 正则 DoS（ReDoS）/ SafeRegex 惯例——嵌套量词与重叠交替的回溯
> 爆炸形态静态扫描，分级防线。

## Problem Statement`

工具/护栏里的正则（PII 模式/注入检测/输入校验）来自各处手写：嵌套
 量词（(a+)+——指数回溯）与重叠交替（(a|ab)——分支翻倍）的 ReDoS
 形态无人审——恶意输入一打即 CPU 打满，防线缺审计面。

## Solution

`RegexRiskAudit`（buzhou-guard，静态纯函数）：

- `audit(pattern)` → `Audit(pattern, risk, findings)`：三形态字符级
  扫描（嵌套量词 `+)+` 类 / 量词组内交替 `(…|…)*` / 重叠交替 `(ab|a)`
  分支共享前缀）；
- 分级：SAFE / SUSPECT（单形态）/ DANGEROUS（多形态叠加——爆炸概率
  叠乘）；转义字符跳过（\+ 不误报）；
- 诚实边界：静态启发式非完备（真完备要 automata 构造——只拦已知形态）。

## User Stories

1. 作为护栏作者，新正则入库前审计——DANGEROUS 拦下、SUSPECT 审查。
2. 作为审计者，findings 直接指出形态与理由——可解释不放黑盒。
3. 作为框架宿主，阈值语义（多形态叠加即高危）固定，纯扫描不拦截。

## Implementation Decisions

- 字符级启发式（诚实边界入档）；转义感知（\ 跳过）。

## Testing Decisions

- 经典 (a+)+ SUSPECT；(a|ab+)+ 三形态 DANGEROUS（首跑红为期望 2 实 3
  ——心算病理第六次实证）；线性/字符类/转义 SAFE 不误报；null
  fail-fast。

## Out of Scope

- 不做 automata 完备分析；不拦截正则使用（决策归宿主）。

## Further Notes

- 与 ToolArgsValidator 互补：那是参数结构校验，这是校验器自身的安全。
