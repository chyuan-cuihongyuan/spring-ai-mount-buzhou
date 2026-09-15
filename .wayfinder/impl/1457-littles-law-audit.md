# impl 1457 — LittlesLawAudit 利特尔法则审计（R57 = effort #1856 / spec 1856 / T2913-T2914）

**What**：`LittlesLawAudit`（core/metrics 静态纯函数）——impliedConcurrency
（λ×W 毫秒换算内置）+ consistency（容差互证，零基线退化绝对口径）；
负值/NaN 四型 fail-fast。

**Why**：排队论 Little's Law 思想——并发/到达率/逗留在稳态下必然互证；
偏差即仪表失真（三处至少一处错）或稳态破（突发积压）——跨指标互证
比单指标自证可信一个量级。

**Verify**：`LittlesLawAuditTest` 4 用例全绿。

**Status**：done（2026-09-16）
