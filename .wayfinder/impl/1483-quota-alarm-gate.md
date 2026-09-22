# impl 1483 — QuotaAlarmGate 配额空间告警门（R83 = effort #1882 / spec 1882 / T2965-T2966）

**What**：`QuotaAlarmGate`（core/policy 持态小门）——onWrite（超
配额触发 NOSPACE 拒写并保持）+ readsAllowed（读永放行）+ usageRatio
（水位可感）+ acknowledge(minFreeBytes)（释放达标才复位）；配额≥1/
阈值≥0/字节非负 fail-fast。

**Why**：etcd NOSPACE 语义——自动恢复写会在清理完成前反复写满
（抖动），「写拒读放 + 恢复须显式确认 + 空闲达标」的保守门免疫
抖动；与弹性预算池（软伸缩）互补。

**Verify**：`QuotaAlarmGateTest` 4 用例全绿（写满两态/读恒放行/
ack 两分支/水位精确与畸形三型 fail-fast）。

**Status**：done（2026-09-23）
