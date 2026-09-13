# impl 572 — EstimatorCalibrationAudit（effort #819）

## 切片

- `buzhou-core/src/main/java/.../core/spi/EstimatorCalibrationAudit.java` — 锁内双账（累计+近窗）+最近秩 P95。
- `buzhou-core/src/test/java/.../core/spi/EstimatorCalibrationAuditTest.java` — 5 例。

## 口径

- relativeError 用 double 累加（锁内——一致性优先）。
- P95 在近窗 |误差| 上（相对误差可为负不取 P95）。

## 验证

mvn -pl buzhou-core -am test -Dtest='EstimatorCalibrationAuditTest' → 5/5 绿；快照再生 1 新公共类型。
