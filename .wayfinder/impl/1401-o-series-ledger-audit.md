# impl 1401 — OSession1800LedgerAuditTest 对账门落位（R1 = effort #1800 / spec 1800 / T2801-T2802）

**What**：starter 测试 `OSession1800LedgerAuditTest`——O 系 150 轮工件链四面
互证：spec（1800–1949 数值号段，跨 18xx/19xx 前缀故区间过滤）↔ 票对
（T2801+2(N−1800) shape / +1 verify）↔ impl（1401+(N−1800)）↔ README 覆盖
（每 spec 号必现）；spec 号从 1800 严格递增。

**Why**：150 轮长会话 + 并行会话共享提交面，号段公式靠记忆必漂移——预防式
对账把公式钉成可执行断言（LSession1700LedgerAuditTest 同款思想），漂移落盘
瞬间即红。

**Verify**：`mvn -pl buzhou-spring-boot-starter test -Dtest=OSession1800LedgerAuditTest`
——4 断言全绿（首跑 1 红「缺 impl 1401」即公式自证生效后补本切片转绿）。

**Status**：done（2026-09-16）
