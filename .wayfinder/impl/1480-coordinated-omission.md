# impl 1480 — CoordinatedOmissionAudit 协同遗漏校正审计（R80 = effort #1879 / spec 1879 / T2959-T2960）

**What**：`CoordinatedOmissionAudit`（core/metrics 静态纯函数）——
correctedSampleCount（观测 1+expectedInterval 阶梯补记）+
omittedCount（被掩盖的发送机会）+ blindWindow（协同静默窗）+
coverageRatio（原始覆盖真实需求比例）；负时延/零间隔/校正减样本
fail-fast。

**Why**：Gil Tene Coordinated Omission——固定速率下慢响应「协同」
推迟后续发送，停顿期延迟样本从未存在，原始分位数偏乐观；阶梯补账
把盲区找回来（450/100 补 4 样本，覆盖率 0.25 即只见四分之一需求）。

**Verify**：`CoordinatedOmissionAuditTest` 4 用例全绿（阶梯四例/
遗漏盲窗三例/覆盖率两例/畸形四型 fail-fast）。

**Status**：done（2026-09-23）
