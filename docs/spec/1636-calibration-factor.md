# 1636 · 校准系数建议（spec 1618 读数可操作化）

> 来源：N 会话 R37（effort #1636 / T2423–T2424 / impl 1189）。

## Solution

`CalibrationAuditHolder.calibrationFactorSuggestion(minSamples)`：
meanRelativeError（= mean((est−act)/act)）一阶换算 `suggested = 1/(1+e)`
（高估 e>0 → 系数 <1 调低估算；低估反之）；pairs < minSamples 或零偏差
empty。只建议不自动改（估算器系数调整是宿主决策——读数给足依据）。

## Testing Decisions

- 三断言：恒高估 25%（20 对）→ 建议 ≈0.8（(0.7,1) 区间）；恒低估 → >1；
  样本不足（5<10）与零偏差 → empty。
- 回归：校准域 10 用例。

## Out of Scope

- 估算器系数的自动热调（闭环控制有振荡风险——建议面先行）。
