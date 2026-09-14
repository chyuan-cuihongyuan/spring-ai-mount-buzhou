# 配置全键表（config-reference）

> 来源：spec 21:9 承诺（map 形态键「由 docs/config-reference 全键表补全」——design-incompleteness F9 闭环，spec 1512）。
> 生成口径：① `@ConfigurationProperties` record 组件（Spring 绑定面，57 个 record 全量在档）；② `fromYml(Map)` 契约子键（经 `ConfigMaps.sub` 桥的模块内 map 键，逐一列于各模块 spec——本表登记键前缀与子键名，语义详注见对应 spec）。
> 组件级默认值/校验语义：见各 record 的 compact constructor（链接列指向源码）。未尽事项：组件级中文语义注记为后续增量轮。
> 键名写法：表中组件名为 record camelCase，yml 绑定用 kebab-case（Spring relaxed binding 双向兼容，如 `scaleUpThreshold` ≡ `scale-up-threshold`）。

## 一、@ConfigurationProperties record 组件（Spring 绑定面）

| 前缀 | 键 | 类型 | 模块 / record |
|------|----|------|--------------|
| `buzhou.bulkhead.scaling` | `scaleUpThreshold` | Long | core / `BulkheadScalingProperties` |
| `buzhou.bulkhead.scaling` | `maxMultiplier` | Integer | core / `BulkheadScalingProperties` |
| `buzhou.alert` | `interval` | Duration | core / `BuzhouAlertProperties` |
| `buzhou.alert` | `rules` | List<RuleSpec> | core / `BuzhouAlertProperties` |
| `buzhou.alert` | `silences` | List<SilenceSpec> | core / `BuzhouAlertProperties` |
| `buzhou.alert` | `inhibitRules` | List<InhibitSpec> | core / `BuzhouAlertProperties` |
| `buzhou.session-archive` | （嵌套结构见源码） | — | core / `BuzhouArchiveProperties` |
| `buzhou.backpressure` | `enabled` | Boolean | core / `BuzhouBackpressureProperties` |
| `buzhou.backpressure` | `maxConcurrentSessions` | Integer | core / `BuzhouBackpressureProperties` |
| `buzhou.backpressure` | `spawnQueueTimeout` | Duration | core / `BuzhouBackpressureProperties` |
| `buzhou.backpressure` | `spawnOverloadPolicy` | String | core / `BuzhouBackpressureProperties` |
| `buzhou.backpressure` | `tool` | Tool | core / `BuzhouBackpressureProperties` |
| `buzhou.backpressure` | `retryBudget` | RetryBudgetParams | core / `BuzhouBackpressureProperties` |
| `buzhou.config-audit` | `enabled` | Boolean | core / `BuzhouConfigAuditProperties` |
| `buzhou.config-audit` | `interval` | Duration | core / `BuzhouConfigAuditProperties` |
| `buzhou` | `modelName` | String | core / `BuzhouCoreProperties` |
| `buzhou` | `store` | Store | core / `BuzhouCoreProperties` |
| `buzhou` | `leaseTtl` | Duration | core / `BuzhouCoreProperties` |
| `buzhou` | `leaseRenewInterval` | Duration | core / `BuzhouCoreProperties` |
| `buzhou` | `lifecycle` | Lifecycle | core / `BuzhouCoreProperties` |
| `buzhou` | `core` | Core | core / `BuzhouCoreProperties` |
| `buzhou.budget.forecast` | `enabled` | Boolean | core / `BuzhouCostForecastProperties` |
| `buzhou.budget.forecast` | `window` | Duration | core / `BuzhouCostForecastProperties` |
| `buzhou.budget.forecast` | `horizon` | Duration | core / `BuzhouCostForecastProperties` |
| `buzhou.budget.forecast` | `budgetMicroUsd` | Long | core / `BuzhouCostForecastProperties` |
| `buzhou.budget.spike` | `enabled` | Boolean | core / `BuzhouCostSpikeProperties` |
| `buzhou.budget.spike` | `baselineBuckets` | Integer | core / `BuzhouCostSpikeProperties` |
| `buzhou.budget.spike` | `minSamples` | Integer | core / `BuzhouCostSpikeProperties` |
| `buzhou.budget.spike` | `zThreshold` | Double | core / `BuzhouCostSpikeProperties` |
| `buzhou.budget.spike` | `floorMicroUsd` | Long | core / `BuzhouCostSpikeProperties` |
| `buzhou.budget.spike` | `cooldown` | Duration | core / `BuzhouCostSpikeProperties` |
| `buzhou.eval.error-sampling` | `enabled` | Boolean | core / `BuzhouErrorSamplingProperties` |
| `buzhou.eval.error-sampling` | `dataset` | String | core / `BuzhouErrorSamplingProperties` |
| `buzhou.eval.error-sampling` | `errorRatePercent` | Integer | core / `BuzhouErrorSamplingProperties` |
| `buzhou.eval.error-sampling` | `minInputChars` | Integer | core / `BuzhouErrorSamplingProperties` |
| `buzhou.eval.sampling` | `enabled` | Boolean | core / `BuzhouEvalSamplingProperties` |
| `buzhou.eval.sampling` | `dataset` | String | core / `BuzhouEvalSamplingProperties` |
| `buzhou.eval.sampling` | `ratePercent` | Integer | core / `BuzhouEvalSamplingProperties` |
| `buzhou.eval.sampling` | `minInputChars` | Integer | core / `BuzhouEvalSamplingProperties` |
| `buzhou.experiments` | `experiments` | Map<String, Map<String, Integer>> | core / `BuzhouExperimentProperties` |
| `buzhou.fsck` | `enabled` | Boolean | core / `BuzhouFsckProperties` |
| `buzhou.fsck` | `interval` | Duration | core / `BuzhouFsckProperties` |
| `buzhou.health.timeline` | `enabled` | Boolean | core / `BuzhouHealthTimelineProperties` |
| `buzhou.health.timeline` | `interval` | Duration | core / `BuzhouHealthTimelineProperties` |
| `buzhou.health.timeline` | `capacity` | Integer | core / `BuzhouHealthTimelineProperties` |
| `buzhou.health.timeline` | `exportPath` | String | core / `BuzhouHealthTimelineProperties` |
| `buzhou.health.timeline` | `exportMaxBytes` | Long | core / `BuzhouHealthTimelineProperties` |
| `buzhou.health.timeline` | `exportMaxHistory` | Integer | core / `BuzhouHealthTimelineProperties` |
| `buzhou.health.timeline` | `exportCompressFrom` | Integer | core / `BuzhouHealthTimelineProperties` |
| `buzhou.latency-slo` | `enabled` | Boolean | core / `BuzhouLatencySloProperties` |
| `buzhou.latency-slo` | `thresholdMillis` | Long | core / `BuzhouLatencySloProperties` |
| `buzhou.latency-slo` | `sloPercent` | Double | core / `BuzhouLatencySloProperties` |
| `buzhou.latency-slo` | `burnRateThreshold` | Double | core / `BuzhouLatencySloProperties` |
| `buzhou.latency-slo` | `window` | Duration | core / `BuzhouLatencySloProperties` |
| `buzhou.latency-slo` | `minSamples` | Integer | core / `BuzhouLatencySloProperties` |
| `buzhou.maintenance` | `from` | Instant | core / `BuzhouMaintenanceProperties` |
| `buzhou.maintenance` | `until` | Instant | core / `BuzhouMaintenanceProperties` |
| `buzhou.maintenance` | `reason` | String | core / `BuzhouMaintenanceProperties` |
| `buzhou.maintenance` | `pollInterval` | Duration | core / `BuzhouMaintenanceProperties` |
| `buzhou.security.message-encryption` | `masterKey` | String | core / `BuzhouMessageEncryptionProperties` |
| `buzhou.security.message-encryption` | `previousMasterKey` | String | core / `BuzhouMessageEncryptionProperties` |
| `buzhou.budget.period` | `enabled` | Boolean | core / `BuzhouPeriodBudgetProperties` |
| `buzhou.budget.period` | `unit` | io.github.chyuan_cuihongyuan.buzhou.core.budget.PeriodBudgetHook.Unit | core / `BuzhouPeriodBudgetProperties` |
| `buzhou.budget.period` | `tokensLimit` | Long | core / `BuzhouPeriodBudgetProperties` |
| `buzhou.budget.period` | `costMicroUsdLimit` | Long | core / `BuzhouPeriodBudgetProperties` |
| `buzhou.budget.period` | `warningPercent` | Integer | core / `BuzhouPeriodBudgetProperties` |
| `buzhou.health.probes` | `livenessMechanisms` | List<String> | core / `BuzhouProbeProperties` |
| `buzhou.health.probes` | `startupMechanisms` | List<String> | core / `BuzhouProbeProperties` |
| `buzhou.prompt` | `templates` | List<TemplateSpec> | core / `BuzhouPromptProperties` |
| `buzhou.prompt.usage-tracking` | `enabled` | Boolean | core / `BuzhouPromptUsageProperties` |
| `buzhou.retention` | `enabled` | Boolean | core / `BuzhouRetentionProperties` |
| `buzhou.retention` | `sweepInterval` | Duration | core / `BuzhouRetentionProperties` |
| `buzhou.retention` | `sessionRetention` | Duration | core / `BuzhouRetentionProperties` |
| `buzhou.retention` | `sessionNotBefore` | Instant | core / `BuzhouRetentionProperties` |
| `buzhou.retention` | `observabilityTtl` | Duration | core / `BuzhouRetentionProperties` |
| `buzhou.retention` | `observabilityBatchSize` | Integer | core / `BuzhouRetentionProperties` |
| `buzhou.retention` | `summaryKeepVersions` | Integer | core / `BuzhouRetentionProperties` |
| `buzhou.retention` | `toolCallLogRetention` | Duration | core / `BuzhouRetentionProperties` |
| `buzhou.retention` | `runCompletedRetention` | Duration | core / `BuzhouRetentionProperties` |
| `buzhou.retention` | `trigger` | Trigger | core / `BuzhouRetentionProperties` |
| `buzhou.runaway` | `enabled` | Boolean | core / `BuzhouRunawayProperties` |
| `buzhou.runaway` | `perTurn` | PerTurn | core / `BuzhouRunawayProperties` |
| `buzhou.runaway` | `perSession` | PerSession | core / `BuzhouRunawayProperties` |
| `buzhou.runaway` | `perTool` | Map<String, PerToolLimit> | core / `BuzhouRunawayProperties` |
| `buzhou.runaway` | `softThresholdRatio` | Double | core / `BuzhouRunawayProperties` |
| `buzhou.runaway` | `repetition` | Repetition | core / `BuzhouRunawayProperties` |
| `buzhou.runaway` | `escalatePolicy` | String | core / `BuzhouRunawayProperties` |
| `buzhou.token-budget` | `enabled` | Boolean | core / `BuzhouTokenBudgetProperties` |
| `buzhou.token-budget` | `maxSessionPromptTokens` | Long | core / `BuzhouTokenBudgetProperties` |
| `buzhou.token-budget` | `maxSessionTotalTokens` | Long | core / `BuzhouTokenBudgetProperties` |
| `buzhou.token-budget` | `maxSessionCostUsd` | BigDecimal | core / `BuzhouTokenBudgetProperties` |
| `buzhou.token-budget` | `pricing` | Map<String, Pricing> | core / `BuzhouTokenBudgetProperties` |
| `buzhou.token-budget` | `warningPercent` | Integer | core / `BuzhouTokenBudgetProperties` |
| `buzhou.tools.deprecated` | `tools` | Map<String, Spec> | core / `BuzhouToolDeprecationProperties` |
| `buzhou.tool-lanes` | `lanes` | Map<String, LaneSpec> | core / `BuzhouToolLaneProperties` |
| `buzhou.tool-lanes` | `tools` | Map<String, ToolBinding> | core / `BuzhouToolLaneProperties` |
| `buzhou.tools.result-schemas` | `schemas` | Map<String, String> | core / `BuzhouToolResultSchemasProperties` |
| `buzhou.tools` | `resultLimitChars` | Integer | core / `BuzhouToolsProperties` |
| `buzhou.tools` | `resultLimitOverrides` | Map<String, Integer> | core / `BuzhouToolsProperties` |
| `buzhou.tools` | `health` | Health | core / `BuzhouToolsProperties` |
| `buzhou.tools` | `circuit` | Circuit | core / `BuzhouToolsProperties` |
| `buzhou.tools` | `baggage` | Map<String, String> | core / `BuzhouToolsProperties` |
| `buzhou.tools` | `inputLimitChars` | Integer | core / `BuzhouToolsProperties` |
| `buzhou.tools` | `inputLimitOverrides` | Map<String, Integer> | core / `BuzhouToolsProperties` |
| `buzhou.ratelimit.turns` | `burst` | Integer | core / `BuzhouTurnRateLimitProperties` |
| `buzhou.ratelimit.turns` | `permitsPerMinute` | Double | core / `BuzhouTurnRateLimitProperties` |
| `buzhou.virtual-keys` | `limits` | Map<String, Long> | core / `BuzhouVirtualKeyProperties` |
| `buzhou.virtual-keys` | `activeKey` | String | core / `BuzhouVirtualKeyProperties` |
| `buzhou.chaos` | `enabled` | Boolean | core / `ChaosProperties` |
| `buzhou.chaos` | `latencyPercent` | Double | core / `ChaosProperties` |
| `buzhou.chaos` | `latencyMillis` | Long | core / `ChaosProperties` |
| `buzhou.chaos` | `exceptionPercent` | Double | core / `ChaosProperties` |
| `buzhou.chaos` | `tools` | List<String> | core / `ChaosProperties` |
| `buzhou.dry-run` | `enabled` | Boolean | core / `DryRunProperties` |
| `buzhou.dry-run` | `tools` | List<String> | core / `DryRunProperties` |
| `buzhou.backpressure.error-budget-freeze` | `enabled` | Boolean | core / `ErrorBudgetFreezeProperties` |
| `buzhou.backpressure.error-budget-freeze` | `interval` | Duration | core / `ErrorBudgetFreezeProperties` |
| `buzhou.error-budget` | `slo` | Double | core / `ErrorBudgetProperties` |
| `buzhou.error-budget` | `window` | Duration | core / `ErrorBudgetProperties` |
| `buzhou.error-budget` | `buckets` | Integer | core / `ErrorBudgetProperties` |
| `buzhou.error-budget` | `burnRateThreshold` | Double | core / `ErrorBudgetProperties` |
| `buzhou.error-budget` | `minSamples` | Integer | core / `ErrorBudgetProperties` |
| `buzhou.runaway.repetition` | `window` | Integer | core / `RepetitionProperties` |
| `buzhou.runaway.repetition` | `similarityPercent` | Double | core / `RepetitionProperties` |
| `buzhou.runaway.repetition` | `unstick` | Boolean | core / `RepetitionProperties` |
| `buzhou.session.disruption-budget` | `minAvailable` | Long | core / `SessionDisruptionBudgetProperties` |
| `buzhou.tool-kill-switch` | `tools` | List<String> | core / `ToolKillSwitchProperties` |
| `buzhou.runaway.tool-loop` | `window` | Integer | core / `ToolLoopProperties` |
| `buzhou.webhook` | `url` | String | core / `BuzhouWebhookProperties` |
| `buzhou.webhook` | `secret` | String | core / `BuzhouWebhookProperties` |
| `buzhou.webhook` | `timeout` | Duration | core / `BuzhouWebhookProperties` |
| `buzhou.webhook` | `maxAttempts` | Integer | core / `BuzhouWebhookProperties` |
| `buzhou.webhook` | `outboxCapacity` | Integer | core / `BuzhouWebhookProperties` |
| `buzhou.webhook` | `queueCapacity` | Integer | core / `BuzhouWebhookProperties` |
| `buzhou.webhook` | `closeDrainTimeout` | Duration | core / `BuzhouWebhookProperties` |
| `buzhou.webhook` | `schema` | Schema | core / `BuzhouWebhookProperties` |
| `buzhou.guard.pii.vault` | `enabled` | Boolean | guard / `BuzhouPiiVaultProperties` |
| `buzhou.guard.pii.vault` | `ttl` | Duration | guard / `BuzhouPiiVaultProperties` |
| `buzhou.guard.pii.vault` | `maxEntries` | Integer | guard / `BuzhouPiiVaultProperties` |
| `buzhou.mcp` | `enabled` | Boolean | mcp / `BuzhouMcpProperties` |
| `buzhou.mcp` | `dangerousToolPatterns` | List<String> | mcp / `BuzhouMcpProperties` |
| `buzhou.mcp` | `shutdownBudget` | Duration | mcp / `BuzhouMcpProperties` |
| `buzhou.mcp` | `perConnectionConcurrencyLimit` | Integer | mcp / `BuzhouMcpProperties` |
| `buzhou.memory.idle-compaction` | `enabled` | Boolean | memory / `IdleCompactionProperties` |
| `buzhou.memory.idle-compaction` | `idleThreshold` | Duration | memory / `IdleCompactionProperties` |
| `buzhou.memory.idle-compaction` | `interval` | Duration | memory / `IdleCompactionProperties` |
| `buzhou.memory.idle-compaction` | `maxPerSweep` | Integer | memory / `IdleCompactionProperties` |
| `buzhou.observe.dashboard` | `enabled` | Boolean | observe-dashboard / `DashboardProperties` |
| `buzhou.observe.dashboard` | `port` | Integer | observe-dashboard / `DashboardProperties` |
| `buzhou.observe.dashboard` | `pathPrefix` | String | observe-dashboard / `DashboardProperties` |
| `buzhou.observe.dashboard` | `bindAddress` | String | observe-dashboard / `DashboardProperties` |
| `buzhou.observe.dashboard` | `authToken` | String | observe-dashboard / `DashboardProperties` |
| `buzhou.observe.otel` | `enabled` | Boolean | observe-otel / `OtelProperties` |
| `buzhou.observe.otel` | `includeContent` | Boolean | observe-otel / `OtelProperties` |
| `buzhou.observe.otel` | `exporterMode` | String | observe-otel / `OtelProperties` |
| `buzhou.observe.otel` | `endpoint` | String | observe-otel / `OtelProperties` |
| `buzhou.observe.otel` | `headers` | Map<String, String> | observe-otel / `OtelProperties` |
| `buzhou.observe.otel` | `timeout` | Duration | observe-otel / `OtelProperties` |
| `buzhou.resilience.model-capabilities` | `models` | Map<String, ModelCapabilities> | resilience / `BuzhouModelCapabilityProperties` |
| `buzhou.resilience.model-concurrency` | `limits` | Map<String, Integer> | resilience / `BuzhouModelConcurrencyProperties` |
| `buzhou.resilience.model-concurrency` | `acquireTimeout` | Duration | resilience / `BuzhouModelConcurrencyProperties` |
| `buzhou.routing` | `weights` | Map<String, Integer> | resilience / `BuzhouRoutingProperties` |
| `buzhou.routing` | `slowStart` | Duration | resilience / `BuzhouRoutingProperties` |
| `buzhou.resilience` | `enabled` | Boolean | resilience / `ResilienceProperties` |
| `buzhou.resilience.idempotency` | `enabled` | Boolean | resilience / `BuzhouIdempotencyProperties` |
| `buzhou.resilience.idempotency` | `ttl` | Duration | resilience / `BuzhouIdempotencyProperties` |
| `buzhou.resilience.idempotency` | `maxEntries` | Integer | resilience / `BuzhouIdempotencyProperties` |
| `buzhou.routing.schedule` | `windows` | List<RoutingWindow> | resilience / `BuzhouRoutingScheduleProperties` |
| `buzhou.routing.schedule` | `checkInterval` | Duration | resilience / `BuzhouRoutingScheduleProperties` |
| `buzhou.resilience.structured-output` | `enabled` | Boolean | resilience / `StructuredOutputProperties` |
| `buzhou.resilience.structured-output` | `maxRepairAttempts` | Integer | resilience / `StructuredOutputProperties` |
| `buzhou.resilience.structured-output` | `schema` | SchemaSpec | resilience / `StructuredOutputProperties` |
| `buzhou.skills` | `enabled` | Boolean | skills / `BuzhouSkillsProperties` |
| `buzhou.skills` | `dbEnabled` | Boolean | skills / `BuzhouSkillsProperties` |
| `buzhou.spill` | `rootDir` | String | spill / `SpillProperties` |
| `buzhou.spill` | `previewChars` | Integer | spill / `SpillProperties` |
| `buzhou.spill` | `listPreviewItems` | Integer | spill / `SpillProperties` |
| `buzhou.spill` | `thresholdChars` | Integer | spill / `SpillProperties` |
| `buzhou.spill` | `thresholdTokens` | Integer | spill / `SpillProperties` |
| `buzhou.spill` | `sandboxRoot` | String | spill / `SpillProperties` |
| `buzhou.spill` | `onloadEnabled` | Boolean | spill / `SpillProperties` |
| `buzhou.spill` | `copyOnWriteEnabled` | Boolean | spill / `SpillProperties` |
| `buzhou.spill` | `offloadEnabled` | Boolean | spill / `SpillProperties` |
| `buzhou.spill` | `editingToolsEnabled` | Boolean | spill / `SpillProperties` |
| `buzhou.spill` | `maxTotalBytes` | Long | spill / `SpillProperties` |
| `buzhou.spill` | `maxFilesPerSession` | Integer | spill / `SpillProperties` |
| `buzhou.spill` | `retentionTtl` | Duration | spill / `SpillProperties` |
| `buzhou.spill` | `encryptionKey` | String | spill / `SpillProperties` |
| `buzhou.store.jdbc` | `dialect` | String | store-jdbc / `JdbcStoreProperties` |
| `buzhou.store.jdbc` | `recoveryEnabled` | Boolean | store-jdbc / `JdbcStoreProperties` |
| `buzhou.store` | `writeFailurePolicy` | WriteFailurePolicy | store-jdbc / `WriteFailurePolicyProperties` |
| `buzhou.leader-election` | `enabled` | Boolean | store-redis / `LeaderElectionProperties` |
| `buzhou.leader-election` | `ttl` | Duration | store-redis / `LeaderElectionProperties` |
| `buzhou.leader-election` | `holderId` | String | store-redis / `LeaderElectionProperties` |
| `buzhou.store.redis` | `uri` | String | store-redis / `RedisStoreProperties` |
| `buzhou.store.redis` | `keyPrefix` | String | store-redis / `RedisStoreProperties` |
| `buzhou.store.redis` | `snapshotTtl` | Duration | store-redis / `RedisStoreProperties` |
| `buzhou.store.redis` | `poolMaxSize` | Integer | store-redis / `RedisStoreProperties` |
| `buzhou.store` | `writeFailurePolicy` | WriteFailurePolicy | store-redis / `WriteFailurePolicyProperties` |

（198 个组件键全量在档。）

## 二、fromYml(Map) 契约子键（ConfigMaps.sub 桥）

各机制模块的内层细键走 `fromYml(Map)` 契约（by-design，spec 21），子键语义详注见对应 spec：

- **guard**（9 个子键，详注见模块 spec）：`auth-ttl`, `canary-guard`, `canary-token`, `dangerous-tools`, `enabled`, `moderation`, `pii`, `secrets`, `spotlighting`
- **mcp**（9 个子键，详注见模块 spec）：`connect-retry`, `enabled`, `force-close-timeout`, `grace-period`, `keepalive-interval`, `max-lifetime`, `poll-interval`, `server-breaker`, `servers`
- **skills**（8 个子键，详注见模块 spec）：`catalog-cache-ttl`, `catalog-max-entries`, `db-enabled`, `enabled`, `hybrid-ranking.enabled`, `hybrid-ranking.lexical-weight`, `scan-locations`, `semantic-ranking.enabled`
- **tools**（1 个子键，详注见模块 spec）：`enabled`

## 三、Environment 直读键（getProperty 字面量）

少量键经 `Environment.getProperty` 直读（不经 record/map），全仓扫描在档：

- `buzhou.cleanup.min-available-sessions`
- `buzhou.core.tool-transient-retry.initial-backoff`
- `buzhou.core.tool-transient-retry.max-attempts`
- `buzhou.core.tool-transient-retry.max-backoff`
- `buzhou.eval.error-sampling.enabled`
- `buzhou.eval.prune.fail-rate-threshold`
- `buzhou.eval.prune.min-items`
- `buzhou.eval.sampling.enabled`
- `buzhou.guard.audit.enabled`
- `buzhou.guard.auto-dangerous-bridge`
- `buzhou.guard.enabled`
- `buzhou.guard.fact-decay.floor`
- `buzhou.guard.fact-decay.half-life-turns`
- `buzhou.guard.pii.vault.salt`
- `buzhou.leak.lease-age-threshold`
- `buzhou.leak.level`
- `buzhou.mcp.enabled`
- `buzhou.memory.enabled`
- `buzhou.metrics.cardinality-guard.enabled`
- `buzhou.model-name`
- `buzhou.resilience.enabled`
- `buzhou.resilience.rate-limit.requests-per-minute`
- `buzhou.resilience.rate-limit.tokens-per-minute`
- `buzhou.routing.slow-start`
- `buzhou.security.message-encryption.master-key`
- `buzhou.security.message-encryption.previous-master-key`
- `buzhou.spill.enabled`
- `buzhou.spill.root-dir`
- `buzhou.store.type`
- `buzhou.virtual-keys.active-key`
- `buzhou.webhook.adaptive-batch`
- `buzhou.webhook.max-payload-chars`
- `buzhou.webhook.rate-limit-burst`
- `buzhou.webhook.rate-limit-per-second`
