# impl 601 — H 会话收口终验（effort #849）

## 收口动作

1. 全反应堆 `mvn -B -ntp clean verify`（JDK21 串行 + Windows 排除集九类）——全仓终验。
2. progress-effort-800.md 台账 Destination 达成（50/50 行回填）。
3. MAP.md #800 总图标已收口。
4. 收口提交 + PR + API merge 入 main。

## 台账对账口径

- specs：docs/spec/800–849 连续 50 份（846 目录 lint、849 收口）。
- 票：T1101–T1200 连续 100 张全闭环（含换题注记 4 轮+补位 1 轮）。
- impl：.wayfinder/impl/553–601 全档（600 为 R48/R49 双档——R49 标「600 续」）。

## 验证

全仓 verify 日志 /tmp/final-verify.log；EXIT=0 后方可提交收口。
