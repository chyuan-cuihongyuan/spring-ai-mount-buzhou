# effort #820 — 护栏豁免登记面

- 会话：H 会话 800 系第 21 轮 ｜ spec [820](../../../docs/spec/820-guard-exemption-registry.md) ｜ 票 [T1141](../tickets/T1141-guard-exemption-registry.md)/[T1142](../tickets/T1142-guard-exemption-registry-verify.md) ｜ impl573
- 借鉴：ESLint suppressions 带过期（eslint/eslint ≈26K star，v9 suppressions file）
- **换题注记**：原 R21 校验错误聚合半撞（ToolArgsValidator 已聚合全部错误为串）——换入豁免登记题。

## 勘察（排重）

- ToolArgsValidator：Optional<String> 全错误串聚合——结构化聚合意义弱（换题）。
- SecretScanner allowlist（T979 域）：扫描器内置——无通用登记面。
- SandboxCommandBackend allowlistedEnv：沙箱环境变量白名单不同域。
- grep -i `exempt|suppression`：无通用命中。

## 决定

`GuardExemptionRegistry`（guard 根包）：grant(mechanism, subject, untilMillis, reason)——机制×主体键显式有时限豁免；exempt(now) 仅未过期 true（now ≥ until 惰性失效计数）；同键覆盖=续期（grantedTotal 照计）；revoke 幂等；封顶 64（满拒新+truncated）；snapshot 仅未过期 until 降序+三口径。脏参数（null/空白/非正 until）返回 false 不抛。护栏 hook 征询与否归 hook 自身接线（默认行为零变化）。

## 测试

登记/判定/机制×主体隔离/撤销幂等/过期惰性失效只计一次/续期覆盖 reason/封顶 64 精确+truncated/脏参数五形态+快照降序——5 例全绿。

## 诚实边界

登记面不自动接线任何 hook（征询语义归 hook——避免隐式放行）；进程内存有界（跨实例共享归 Redis 族）；subject 语义由调用方定义（本类不解释）。
