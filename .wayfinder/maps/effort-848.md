# effort #848 — 配置默认偏离审计

- 会话：H 会话 800 系第 49 轮 ｜ spec [848](../../../docs/spec/848-config-deviation-audit.md) ｜ 票 [T1197](../tickets/T1197-config-deviation-audit.md)/[T1198](../tickets/T1198-config-deviation-audit-verify.md) ｜ impl600 续
- 借鉴：Spring Boot configuration metadata 思想扩散（spring-projects/spring-boot ≈78K；ConfigDiff 是快照间 diff——本类 vs 出厂默认）

## 勘察（排重）

- ConfigDiff/ConfigDriftAuditor：两次快照间 diff——vs 默认基线缺位。
- ConfigDoctor/ConfigFingerprint：健康诊断/指纹——偏离清单不同面。
- grep -i `deviation|默认偏离`：无命中。

## 决定

`ConfigDeviationAudit`（core.config，纯函数）：audit(currents, defaults)——String.equals 值比较（类型归一化归调用方）；默认缺失键不计 configured（无基线不裁决）；偏离清单典序封顶 32；空值键跳过不计；空配置空真 0。喂点=starter 装配侧（元数据默认值采集）。

## 测试

偏离判定+无基线跳过+偏离率 0.5/清单典序封顶 37→32/空值键跳过空真——3 例绿（测试两处笔误修正：误把无基线键放进 defaults、负数键名破坏典序）。

## 诚实边界

值比较字符串相等（类型归一化归调用方）；默认值基线由调用方采集（元数据来源不绑定）；无基线键不计偏离也不计 configured（口径显式）。
