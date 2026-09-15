# impl 1332 — SpanAttributeBudget span 属性预算审计（R33 = effort #1732 / spec 1732 / T2665-T2666）

**What**：逐 span 记账+128 属性/8192 字节可调阈值+超限与极值
**Why**：OTel 属性限额——管道内存与后端基数守护
**Verify**：SpanAttributeBudgetTest 3 断言 全绿。 **Status**：done（2026-09-15）
