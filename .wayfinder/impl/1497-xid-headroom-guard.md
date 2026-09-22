# impl 1497 — XidHeadroomGuard 事务号余量分级（R97 = effort #1896 / spec 1896 / T2993-T2994）

**What**：`XidHeadroomGuard`（core/recovery 静态纯函数 + Urgency
枚举）——urgency 四级判定（OK/WARN/CRITICAL/EXHAUSTED 边界含上）+
headroom 余量读数（超发负值诚实显示）；分级线单调/越界/负消耗
fail-fast。

**Why**：Postgres xid wraparound 防线——单调编号耗尽前无分级预警，
到耗尽即以拒绝一切写入暴雷；分级给清理窗口刻度（WARN 排期/
CRITICAL 动手/EXHAUSTED 停写自保）。与 QuotaAlarmGate 互补。

**Verify**：`XidHeadroomGuardTest` 4 用例全绿（四级/边界含上/余量
读数/畸形三型 fail-fast）。

**Status**：done（2026-09-23）
