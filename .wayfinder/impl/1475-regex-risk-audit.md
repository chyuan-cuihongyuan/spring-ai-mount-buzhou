# impl 1475 — RegexRiskAudit 正则风险审计（R75 = effort #1874 / spec 1874 / T2949-T2950）

**What**：`RegexRiskAudit`（buzhou-guard 静态纯函数）——三形态启发式
（嵌套量词/量词组交替/重叠交替，转义感知）+ 三级分级（多形态叠乘即
DANGEROUS）+ findings 可解释；null fail-fast。

**Why**：OWASP ReDoS 防线思想——嵌套量词指数回溯、重叠交替分支翻倍；
手写正则入库前审计，DANGEROUS 拦下 SUSPECT 审查；诚实边界静态非完备。

**Verify**：`RegexRiskAuditTest` 4 用例全绿（首跑红为心算期望误——R54
病理第六次实证）。

**Status**：done（2026-09-16）
