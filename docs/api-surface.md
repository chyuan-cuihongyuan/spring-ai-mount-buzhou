# Public API Surface（模块 × 公开类型清单）

> effort #5 / T100 / impl-75 自动生成 + 人工整理。审计口径：src/main 下非 `internal` 包的
> public class/interface/record/enum。`internal` 包类型不属公开 API（可任意变更，见 CONTRIBUTING 稳定性政策）。

## buzhou-core

- `public class BuzhouConfigurationException`
- `public class BuzhouCoreAutoConfiguration`
- `public class BuzhouDataCorruptionException`
- `public class BuzhouException`
- `public class BuzhouStoreFailureAnalyzer`
- `public class CompositeAttachmentRenderer`
- `public class FileSandbox`
- `public class HarnessToolCallingManager`
- `public class HookChain`
- `public class LeaseLostException`
- `public class QuotaExceededException`
- `public class RetentionSweeper`
- `public class RunRecoveryService`
- `public class RunStateTrackerHook`
- `public class RunawayBudgetRenderer`
- `public class RunawayHook`
- `public class SandboxViolationException`
- `public class SessionAlreadyActiveException`
- `public class SessionCapacityExceededException`
- `public class StructuredOutputException`
- `public class TokenBudgetHook`
- `public enum CancelMode`
- `public enum ErrorCode`
- `public enum OnFail`
- `public enum OverloadPolicy`
- `public enum RetryCategory`
- `public enum Role`
- `public enum RunStatus`
- `public enum ToolCallOutcome`
- `public enum ToolFeedbackType`
- `public enum Transport`
- `public final class BuzhouHealthEndpoint`
- `public final class BuzhouHealthIndicator`
- `public final class BuzhouLifecyclePhases`
- `public final class BuzhouMetricsBinder`
- `public final class BuzhouMetricsHolderInstaller`
- `public final class BuzhouMetricsHolder`
- `public final class BuzhouThreadFactory`
- `public final class Buzhou`
- `public final class CachedEmbeddingProvider`
- `public final class CancellationToken`
- `public final class ConfigMaps`
- `public final class EventType`
- `public final class LeakDetectorHolder`
- `public final class MicrometerBuzhouMetrics`
- `public final class RecoverySupport`
- `public final class ResourceLeakDetector`
- `public final class RunawayCounters`
- `public final class SessionCleaner`
- `public final class SessionInterrupts`
- `public final class SpanContextCarrier`
- `public final class SpanKind`
- `public final class SpanStatus`
- `public final class SpawnGate`
- `public final class Spotlighting`
- `public final class ToolArgsValidator`
- `public final class ToolErrorFeedback`
- `public final class ToolPolicyMatcher`
- `public final class ToolValidationFeedback`
- `public final class WebhookEventForwarder`
- `public interface AgentRuntime`
- `public interface AgentSession`
- `public interface AttachmentRenderer`
- `public interface BindingPolicyChangeListener`
- `public interface BindingPolicyStore`
- `public interface BuzhouHealth`
- `public interface BuzhouHook`
- `public interface BuzhouMetrics`
- `public interface CommandBackend`
- `public interface ContextWindowResolver`
- `public interface EmbeddingProvider`
- `public interface FactStore`
- `public interface HookContext`
- `public interface MemoryViewProcessor`
- `public interface MessageStore`
- `public interface ModelCallContext`
- `public interface ObservabilityStore`
- `public interface PolicyConfigProvider`
- `public interface RunRegistry`
- `public interface SessionAssemblyContext`
- `public interface SessionAssemblyCustomizer`
- `public interface SessionEventContext`
- `public interface SessionEventListener`
- `public interface SessionLeaseStore`
- `public interface SessionObserver`
- `public interface SessionResourceCustomizer`
- `public interface SessionStateHandle`
- `public interface SessionStateStore`
- `public interface SkillCatalogRenderer`
- `public interface SkillResourceResolver`
- `public interface SpanHandle`
- `public interface SpanRecorder`
- `public interface SummaryStore`
- `public interface TokenEstimator`
- `public interface ToolCallContext`
- `public interface ToolCallLog`
- `public interface ToolSetProvider`
- `public interface TurnContext`
- `public interface TurnLoopContext`
- `public interface UnitOfWork`
- `public record BindingPolicy`
- `public record BuzhouBackpressureProperties`
- `public record BuzhouCoreProperties`
- `public record BuzhouMessage`
- `public record BuzhouRetentionProperties`
- `public record BuzhouRunawayProperties`
- `public record BuzhouStores`
- `public record BuzhouTokenBudgetProperties`
- `public record BuzhouToolsProperties`
- `public record BuzhouWebhookProperties`
- `public record ClosedSession`
- `public record EventBusStats`
- `public record EventDispatchConfig`
- `public record EventRecord`
- `public record Fact`
- `public record InMemoryStoreConfig`
- `public record InjectionSnapshot`
- `public record LayeredPolicy`
- `public record LeaseAcquireResult`
- `public record LeaseInfo`
- `public record MaintenanceTrigger`
- `public record McpServerBinding`
- `public record ObservabilityTtl`
- `public record RetentionSweepReport`
- `public record RunStateSnapshot`
- `public record RuntimeConfig`
- `public record SessionCleanupContributor`
- `public record SessionCleanupResult`
- `public record SessionEvent`
- `public record SessionHistoryPolicy`
- `public record SessionSummary`
- `public record SnapshotMessage`
- `public record SpanContext`
- `public record SpanRecord`
- `public record SpawnOptions`
- `public record StateEntry`
- `public record StructuredSummary`
- `public record ToolCallLogEntry`
- `public record ToolCallRecord`
- `public record ToolSetSpec`
- `public record TurnDeadline`
- `public record TurnLoopPolicy`
- `public sealed interface HookResult`

## buzhou-memory

- `public class BiTemporalFactLedger`
- `public class BuzhouMemoryAutoConfiguration`
- `public class BuzhouMemoryHealthAutoConfiguration`
- `public class CompactNowTool`
- `public class DefaultBudgetCalculator`
- `public class DefaultCompletedTurnDetector`
- `public class DefaultMicroCompactor`
- `public class DefaultSummaryDegrader`
- `public class DefaultSummaryGenerator`
- `public class EvidenceLookupTool`
- `public class InjectionViewProcessor`
- `public class MemoryModuleLifecycle`
- `public class RecallSearchTool`
- `public class ReviseSummarySectionTool`
- `public class SleepTimeConsolidationHook`
- `public class SleepTimeConsolidator`
- `public class SummaryCircuitBreaker`
- `public class SummaryFactReconciler`
- `public class SummaryStoreBridge`
- `public enum SummarySection`
- `public final class CompactionCheckpoints`
- `public final class CompactionFidelityEval`
- `public final class EpisodeLedger`
- `public final class ManualCompactor`
- `public final class MemoryHealth`
- `public final class MemoryModule`
- `public final class RecallSearch`
- `public final class SegmentBudgetPlanner`
- `public final class SessionForks`
- `public final class SleepTimeScheduler`
- `public interface BudgetCalculator`
- `public interface CompletedTurnDetector`
- `public interface MicroCompactor`
- `public interface SummaryDegrader`
- `public interface SummaryGenerator`
- `public record BudgetInput`
- `public record BudgetReport`
- `public record MicroCompactionPolicy`
- `public record MicroCompactionResult`
- `public record NineSectionSummary`
- `public record SectionContent`
- `public record TurnSpan`

## buzhou-spill

- `public class BuzhouSpillAutoConfiguration`
- `public class BuzhouSpillHealthAutoConfiguration`
- `public class CopyFileTool`
- `public class CopyOnWriteGuardHook`
- `public class DiskSpillStore`
- `public class EvictHandleTool`
- `public class FileSandbox`
- `public class HotTailViewProcessor`
- `public class OnloadHook`
- `public class ReadRangeTool`
- `public class SandboxViolationException`
- `public class SessionReadOnlyRegistry`
- `public class SpillModuleLifecycle`
- `public class SpillOffloadHook`
- `public class SpillService`
- `public class StrReplaceTool`
- `public final class ContentSlicer`
- `public final class HandleLifecycleRegistry`
- `public final class LongContentParams`
- `public final class RangeReadEngine`
- `public final class ReadIntegrity`
- `public final class SemanticChunkIndex`
- `public final class SpillGuardModule`
- `public final class SpillHealth`
- `public final class SpillModule`
- `public interface SpillStore`
- `public record LongContentParamPair`
- `public record RangeReadRequest`
- `public record RangeReadResult`
- `public record SpillEntry`
- `public record SpillHandle`
- `public record SpillProperties`
- `public record SpillQuota`
- `public record SpillUri`

## buzhou-observability

- `public abstract class BaseSpanRecorder`
- `public class AsyncObservabilityPipeline`
- `public class BuzhouObservabilityAutoConfiguration`
- `public class DefaultSpanHandle`
- `public class MicrometerDualWriter`
- `public class ObservabilityAdvisor`
- `public class ObservabilitySessionState`
- `public class ObservableToolCallback`
- `public class SynchronousObservabilityPipeline`
- `public class ThinkingChainExtractor`
- `public final class ObservabilityModule`
- `public interface PipelineSink`
- `public record ExtractedThinking`
- `public record FlushToken`
- `public record ObservabilityConfig`
- `public record PendingEvent`
- `public record PendingSnapshot`
- `public record PendingSpan`
- `public sealed interface PendingItem`

## buzhou-observe-otel

- `public class BuzhouOtelAutoConfiguration`
- `public final class OtelBridge`
- `public record OtelBridgeConfig`
- `public record OtelProperties`

## buzhou-observe-dashboard

- `public class BuzhouDashboardAutoConfiguration`
- `public class DashboardModule`
- `public class DashboardQueryService`
- `public interface SkillAdminPort`
- `public record DashboardProperties`

## buzhou-skills

- `public class BuzhouSkillsAutoConfiguration`
- `public class ClasspathSkillScanner`
- `public class DefaultSkillRegistry`
- `public class InMemorySkillStore`
- `public class JdbcSkillStore`
- `public class LoadSkillTool`
- `public class RedisSkillStore`
- `public class SessionBindingIndex`
- `public class SkillAdminApi`
- `public class SkillCatalogRendererImpl`
- `public class SkillVersionConflictException`
- `public enum SkillSource`
- `public enum SkillStatus`
- `public final class SkillModule`
- `public interface SkillRegistry`
- `public interface SkillStore`
- `public record BuzhouSkillsProperties`
- `public record ClasspathSkillEntry`
- `public record DbSkillRecord`
- `public record DbSkillResourceRecord`
- `public record ParsedSkillMd`
- `public record SkillFrontmatter`
- `public record SkillMetadata`
- `public record SkillResource`
- `public record SkillSummary`
- `public record Skill`

## buzhou-mcp

- `public class BuzhouMcpAutoConfiguration`
- `public class BuzhouMcpHealthAutoConfiguration`
- `public class DbToolSetProvider`
- `public class InMemoryToolSetSpecStore`
- `public class JdbcToolSetSpecStore`
- `public class PropertiesToolSetProvider`
- `public class SpringAiMcpConnectionFactory`
- `public final class McpModule`
- `public interface McpClientRegistry`
- `public interface McpConnectionFactory`
- `public interface McpConnection`
- `public interface ToolSetSpecStore`
- `public record BuzhouMcpProperties`

## buzhou-guard

- `public class BuzhouGuardAutoConfiguration`
- `public class BuzhouGuardHealthAutoConfiguration`
- `public class CanaryGuardHook`
- `public class DangerousToolGuardHook`
- `public class FactAttachmentRenderer`
- `public class FactCollectorHook`
- `public class GuardAuthApi`
- `public class GuardModuleLifecycle`
- `public class PolicyGateHook`
- `public class SpotlightHook`
- `public class TaintTrackingHook`
- `public class TaintWriteGateHook`
- `public enum AuthTtl`
- `public final class ArgumentFingerprint`
- `public final class AuditChainVerifier`
- `public final class AuditChain`
- `public final class AuditTrailCollector`
- `public final class DangerousToolMatcher`
- `public final class DenoSandbox`
- `public final class E2BSandbox`
- `public final class EmbeddedPolicyEngine`
- `public final class FirecrackerSandbox`
- `public final class GuardHealth`
- `public final class GuardModule`
- `public final class InMemoryAuditRecordStore`
- `public final class Jcs`
- `public final class JdbcAuditRecordStore`
- `public final class LimitedCommandSandbox`
- `public final class OnnxPromptGuard`
- `public final class PemFileKeyProvider`
- `public final class PolicyRefresher`
- `public final class PolicyRuleParser`
- `public final class ResourcePolicySource`
- `public final class SandboxCommandBackend`
- `public final class SigningKeyRing`
- `public interface AuditRecordStore`
- `public interface CommandSandbox`
- `public interface FactDefinition`
- `public interface InjectionClassifier`
- `public interface PolicyEngine`
- `public interface PolicySource`
- `public interface SandboxProcessLauncher`
- `public interface SigningKeyProvider`
- `public record AgentAuditRecord`
- `public record ConfirmOption`
- `public record Confirmation`
- `public record DangerousToolConfig`
- `public record DangerousToolEntry`
- `public record GuardAuditConfig`
- `public record GuardPolicyConfig`
- `public record PolicyDecision`
- `public record SandboxLimits`
- `public record VerificationReport`

## buzhou-tools

- `public class BuzhouToolsAutoConfiguration`
- `public class CommandBlacklist`
- `public class HttpRequestTool`
- `public class ReadFileTool`
- `public class RunCommandTool`
- `public class SandboxRunCommandTool`
- `public class SsrfGuard`
- `public class TodoAttachmentRenderer`
- `public class TodoStore`
- `public class TodoTool`
- `public class WriteFileTool`
- `public final class ToolsModule`
- `public record LongContentParamDecl`
- `public record RunCommandArgs`
- `public record TodoItem`

## buzhou-resilience

- `public class BuzhouResilienceAutoConfiguration`
- `public class BuzhouResilienceHealthAutoConfiguration`
- `public class DefaultErrorClassifier`
- `public class ModelCallInFlight`
- `public class ModelCallTimeoutException`
- `public class ModelRateLimitExceededException`
- `public class RateLimitAdvisor`
- `public class ResilienceAdvisor`
- `public class ResilienceSessionObserver`
- `public class SessionQuotaHook`
- `public enum CircuitState`
- `public enum ErrorCategory`
- `public final class FallbackChain`
- `public final class ModelCircuitBreaker`
- `public final class ModelCircuitOpenException`
- `public final class ModelConcurrencyAdvisor`（426——Resilience4j SemaphoreBulkhead/
  Uber concurrency-limits：per-model 在飞并发舱——链序 +660 许可持有跨
  重试、流式 doFinally 释放含 CANCEL）
- `public final class ModelConcurrencyHotReload`（429——320/340 rebind 同
  模式：refresh 事件重读 limits 热调容——在飞不受扰自然收敛）
- `public final class ModelConcurrencyLimiter`
- `public final class ModelRateLimiter`
- `public final class ResilienceModule`
- `public final class ResilienceStats`
- `public interface ProviderErrorClassifier`
- `public record Classification`
- `public record NamedFallbackModel`
- `public record ResilienceProperties`

## buzhou-store-jdbc

- `public class BuzhouJdbcStoreAutoConfiguration`
- `public class DegradingObservabilityStore`
- `public class JdbcMessageStore`
- `public class JdbcObservabilityStore`
- `public class JdbcRunRegistry`
- `public class JdbcSessionLeaseStore`
- `public class JdbcSessionStateStore`
- `public class JdbcSummaryStore`
- `public class JdbcToolCallLog`
- `public class JdbcUnitOfWork`
- `public enum Dialect`
- `public enum WriteFailurePolicy`
- `public final class JdbcBuzhouStores`
- `public final class SchemaMigrator`
- `public record JdbcBuzhouRecoveryStores`
- `public record JdbcStoreProperties`
- `public record WriteFailurePolicyProperties`

## buzhou-store-redis

- `public class BuzhouRedisStoreAutoConfiguration`
- `public class DegradingObservabilityStore`
- `public class RedisMessageStore`
- `public class RedisObservabilityStore`
- `public class RedisSessionLeaseStore`
- `public class RedisSessionStateStore`
- `public class RedisSummaryStore`
- `public class RedisUnitOfWork`
- `public enum WriteFailurePolicy`
- `public final class RedisBuzhouStores`
- `public record RedisStoreProperties`
- `public record WriteFailurePolicyProperties`

## buzhou-spring-boot-starter

- （纯依赖聚合模块：无 src/main 代码，无公开类型；引入即得全部机制自装配）

## internal 包审计（public 修饰但非公开 API）

36 个类型位于 `*.internal.*` 包且声明为 public——**实现细节，不属公开 API**（包可见性受模块
边界约束；Java 无包私有跨文件包结构强制，internal 命名 + 本清单即契约）。变更不通知、不迁移。

## 稳定性政策（同步 CONTRIBUTING）

- **公开 API**（本清单类型）：语义化版本；minor 可加不可改，major 才可破坏。
- **@since 标注**：新公开类型自 1.0.0 起标（当前 0.1.0-SNAPSHOT 预发布期不追溯补标）。
- **deprecation**：废弃保留 ≥ 2 个 minor，javadoc `@deprecated` 指明替代。
- **internal 包 / core ConfigMaps 模块私有 map 契约**：不受上述政策约束。

## effort #6 新增公共面（spec 24–32 / impl-78–86，@since 1.0.0）

**buzhou-core**

- `public record MediaRef`（session）+ `AgentSession.chat/stream/chatForEntity` 媒体重载（default UOE）
- `public record SessionExport`（session）+ `AgentRuntime.exportSession/importSession`（default UOE）
- `public class SessionImportException`（session）
- `public interface SessionForkListener`（session）+ `RuntimeConfig` 第 11 槽 forkListeners（10 参构造保留）
- `public class StoreFsck` / `public final class StoreIntegrityReport`（cleanup；含 Finding/Severity）
- `public record SessionInfo` / `public record SessionIndexQuery` / `public interface SessionIndexStore`（spi）
- `public final class ToolResultLimiter` / `public final class ToolResultLimiterHolder`（exec）
- `public record WebhookDeadLetter`（webhook）；`WebhookOutbox` 为包私有（非公开面）；
  `WebhookSignatures`（428——Stripe signed webhooks：消费端常量时间验签
  +时间戳容差窗防重放，forwarder 加发 X-Buzhou-Timestamp 不进 MAC）
- **破坏性变更（pre-1.0）**：`WebhookEventForwarder` 构造改双参（props, SessionStateStore）；
  `BuzhouWebhookProperties` 增 `outboxCapacity`（6 参）；`queueCapacity` 废弃 no-op；
  `ResilienceProperties.Circuit` 增 `backoffCap`（6 参便捷构造保留）；
  `BuzhouMessage.metadata` 新键约定 `mediaRefs`（非 schema 变更）。

**buzhou-store-jdbc / buzhou-store-redis**

- `public class JdbcSessionIndexStore` / `public class RedisSessionIndexStore`（含 `create` 工厂；
  auto-config 于 store.type=jdbc/redis 时装配 `SessionIndexStore` bean）

**测试面（core test-jar，非运行时 API）**

- `public final class EventSequenceAssert`（testsupport；attach/attachGlobal + 序列断言族）

## effort #7 新增公共面（spec 33–36 / impl-87–104，@since 1.0.0）

**buzhou-core**

- `SessionStateStore.scanByPrefix(sessionId, prefix)` default 方法（JDBC/Redis 覆写下推）
- `AgentRuntime` 无新签名；`DefaultAgentRuntime.setExportExtensions(List<SessionExportExtension>)`（internal 装配面）
- `SessionExportExtension` 接口 + `SessionExport` 第 9 槽 `extensions`（8 参构造兼容）
- `ToolResultLimiter.limitFor` 转公共（生效上限查询面）；`ToolResultLimiterHolder`
- `StoreFsck.run(stores, SessionIndexStore, extras)` 三参重载
- **破坏性变更（pre-1.0）**：`DashboardQueryService` 增双参构造（单参保留）；
  `SkillRegistry.listForPage` default 方法 + `CatalogPage`；`SessionCleaner` 无变化（贡献者经 auto-config 挂接）

**buzhou-memory / buzhou-spill / buzhou-observe-dashboard**

- `FactsExporter implements SessionExportExtension`（memory.facts 段）
- `MediaIntake`（intake/readBack 二进制无损 + intakeText/readBackText）
- `DashboardQueryService.listSessionsFiltered` + `IndexedSessionPage` + `Builder.sessionIndex`

**测试面（core test-jar，非运行时 API）**

- `AbstractSessionIndexContractTest`（契约矩阵基类）；`WebhookOutboxPerfAccess`（outbox 直驱桥）；
  `EventSequenceAssert` 既有（effort #6）

## effort #8 新增公共面（spec 37–39 / impl-105–118，@since 1.0.0）

**buzhou-core**

- `SkillSearchTool`（skills 模块，ToolCallback 直实现）+ `SkillRegistry.listAllFor` default（不截断全集）
- `WebhookEventForwarder.replayDeadLetters()`；`WebhookOutboxHealth`（forwarder 装配时注册）
- `SessionIndexStore.purgeOlderThan(cutoff, limit)` default（三实现覆写）+
  `SessionIndexObserver.configureRetention`；`BuzhouCoreProperties.Core.indexClosedRetention`
  （buzhou.index.closed-retention，默认 30d）
- `SessionMigrator.migrate(source, target, sessionId, keepIds)` 静态工具
- `CompactionListener`（memory；onCompacted(sessionId, result, evictRatio)——替代 T115 BiConsumer）
- `SessionIndexHealth`（SessionIndexStore bean 存在时注册）
- **破坏性变更（pre-1.0）**：`InjectionViewProcessor.setCompactionListener` 签名改
  `CompactionListener`（三参演化）；`ObservabilityConfig`/`RuntimeConfig` 无变化

**buzhou-memory / buzhou-spill**

- `FactsExporter`（既有）；`MediaIntake`（既有）——@since 补齐

**测试面（core test-jar）**

- `WebhookOutboxPerfAccess` 增 `requeueDead(limit)` + `SESSION_ID` 常量


## effort #9 新增公共面（spec 40–45 / impl-122–135，@since 1.0.0）

**buzhou-spill**

- `SpillCipher`（fromBase64Key/encrypt/decryptIfEncrypted/isEncrypted；MAGIC 常量）
- `DiskSpillStore` 三参构造（rootDir, quota, cipher；cipher null = 直通）
- `SpillModule` 五参构造（带 cipher）
- `SpillProperties.encryptionKey`（第 14 组件；非法密钥构造期 fail-fast）

**buzhou-core**

- `ErrorCode.TURN_IN_FLIGHT`（NON_RETRYABLE）——单飞闸确定拒绝
- `ReadDegradePolicy`（OFF/EMPTY）+ `ReadDegradeHolder`（全局默认；spi 包）
- `BuzhouCoreProperties.Store.readDegrade`（第 3 组件）+ `readDegradePolicy()`；
  auto-config 增 `buzhouReadDegradePolicy` 初始化 bean
- `BuzhouWebhookProperties.closeDrainTimeout`（第 7 组件）+ `effectiveCloseDrainTimeout()`
- `BuzhouRunawayProperties`/`BuzhouBackpressureProperties` 全键构造期 fail-fast（null=不限语义保留）
- **破坏性变更（pre-1.0）**：webhook `maxAttempts`/`outboxCapacity` 非法值由静默回退默认改
  `BuzhouConfigurationException`；`VerificationReport` 增第 6/7 组件（headHash/anchorMatched，
  5 参兼容构造保留）

**buzhou-guard**

- `SigningKeyPersister` 接口 + `PemFileKeyPersister`（privateKeyFile/publicKeyFile 工厂）
- `SigningKeyRing` 三参构造（带 persister；rotate 写而后切）
- `PemFileKeyProvider.scanDirectory(Path)` 静态工厂
- `AuditChainVerifier.verify(records, ring, expectedHeadAnchor)` 三参重载；
  `VerificationReport.anchored()`
- `GuardAuditConfig.keyDir`（第 6 组件）

**buzhou-resilience**

- `ModelCircuitBreaker`/`SessionQuotaHook` 三参构造（可注入 Clock；缺省 systemUTC）
- `MetricTags.bound(String)`（指标 tag 32 字符截断纪律公用）

**buzhou-store-jdbc**

- SchemaMigrator：版本表 checksum 列 + 未来版本拒绝 + `validateChecksums`（无公开 API 变更；
  行为契约入 spec 42 §A）

**buzhou-tools**

- `RunCommandTool` 七参构造（maxOutputBytes）+ `DEFAULT_MAX_OUTPUT_BYTES`（5MB 公开常量）
- `ToolsModule`：`run-command.max-output-bytes` yml 键（非正 fail-fast）

**buzhou-memory**

- `SleepTimeScheduler` 四参构造（closeGrace；close 优雅排空→硬截断）

**构建面**

- enforcer 第二执行段：dependencyConvergence + banDuplicatePomDependencyVersions；
  `com.networknt:json-schema-validator` 钉 3.0.1（Spring AI 双路传递分歧收口）

## effort #10 新增公共面（spec 46–51 / impl-139–153，@since 1.0.0）

**buzhou-observability**

- `ObservabilityAdvisor`：TTFT/TPOT 首内容信号打点（span 属性 + STREAM_FIRST_TOKEN 事件；
  空块不触发；非流式零变化）
- `buzhou.model.ttft` / `buzhou.model.tpot` Timer（`BuzhouMetricsBinder` 预注册；model tag 截断）

**buzhou-core**

- `AgentSession.rateTurn(turnSeq, type, value, comment, source)` default 方法（不支持实现抛 UOE）
- `FeedbackExporter`（core.feedback 导出扩展段：负反馈标记 + negativeTurnSeqs 汇总；空段缺席）
- `EventType.STREAM_FIRST_TOKEN` 常量；`turn.feedback` 会话事件（webhook 监听者零改造）
- `buzhou.stream.cancelled{client|deadline|guard}` 计数 + `StreamTotalTimeoutException`（internal；
  对外语义经轮次失败面呈现）
- `BuzhouCoreProperties.Core.streamTotalTimeout`（第 4 组件；null = 10m，≤0 = ZERO 哨兵关闭；
  3 参兼容构造保留）
- `ErrorCode` 新增 `SPILL_IO_FAILED` / `STORE_READ_FAILED` / `SKILL_OPERATION_INVALID`
- **破坏性变更（pre-1.0）**：泛化 throw 渐进挂码——spill IO 面（9 处）、store 读取面、技能管理面
  （4 处）、todo/SHA（2 处）由 ISE/泛化 RuntimeException 改抛带 ErrorCode 的 `BuzhouException`；
  断言类 ISE 保留面钉住不迁（catch 具体异常类型的调用方需跟进）

**buzhou-resilience**

- `ResilienceProperties.Fallback`：`canaryEnabled` / `weights` 第 3/4 组件（2 参兼容构造保留）
- `ResilienceProperties.Shadow` 参数组（`shadow.enabled/models/max-concurrent/daily-budget`）；
  顶层 record 第 13 组件（12 参兼容构造保留）
- `ShadowTrafficController`（异步对照提交 + 并发/日预算护栏；`EVENT_COMPARED = "shadow.compared"`）
- `ModelRateLimiter`：降级/金丝雀候选过闸 + 按实际服务模型 TPM 记账 + remaining gauge
- **破坏性变更（pre-1.0）**：`Fallback`/`ResilienceProperties` canonical 构造组件数增加
  （兼容构造保留，源码兼容；反射按 canonical 构造绑定的调用方需核对）

**buzhou-spill / buzhou-skills / buzhou-tools**

- `DiskSpillStore` / `SkillAdminApi` / `TodoStore` 泛化异常挂码（对齐上文错误码收口；
  无签名变化）

## effort #11 新增公共面（spec 52 / impl-156–165，@since 1.0.0）

**buzhou-core（io…buzhou.core.eval 包，全新）**

- `EvalDatasetStore`（createDataset/listDatasets/dataset/addItem/items/deleteDataset；
  合成会话 `__buzhou.eval__`）
- `EvalDatasetMeta` / `EvalItem`（溯源 sourceSessionId+sourceTurnSeq）
- `FeedbackImporter`（importFromFeedback；`FeedbackImportResult{imported, skippedDuplicate,
  skippedMissingReply}`）
- `Evaluator`（SPI）/ `EvalScore`（passed+detail 512 截断）/ `BuiltInEvaluators`
  （EXACT/CONTAINS 常量 + regex(String) 工厂）
- `EvalRunner`（run(datasetName, evaluator)）/ `EvalRunResult`（passRate）/ `EvalRunItemResult`
- `EvalQueryService`（allRuns/runs/run/latestRun；只读）
- `FeedbackExporter.isNegative` / `decode` 由包内提 `public`（回流单一事实源口径；行为零变化）

**buzhou-core（session 面）**

- `AgentSession.emitEvent(type, payload)` default 方法（default UOE；DefaultAgentSession 实现；
  `eval.run.completed` 事件经此通道外发）
- `ErrorCode.EVAL_OPERATION_INVALID`（NON_RETRYABLE）
- **破坏性变更（pre-1.0）**：无（纯新增面；emitEvent default 不破坏既有实现）

## effort #12 新增公共面（spec 53 / impl-168–175，@since 1.0.0）

**buzhou-resilience（resilience.cache 包，全新）**

- `ResponseCacheAdvisor`（BaseAdvisor；order +450；`isTerminal(ChatResponse)` 公开终态判定）
- `ResponseCacheStore`（LRU+TTL 惰性过期；hit/miss/evicted 计数可读；可注入 Clock）
- `ResponseCacheKeys`（`keyOf(modelName, Prompt)` 键计算；options 采样近似性 javadoc 入档）
- `ResilienceProperties.ResponseCache` 参数组（顶层 record 第 14 组件；13 参兼容构造保留）
- yml 键：`buzhou.resilience.response-cache.{enabled,max-entries,ttl}`（metadata 已入档）
- **破坏性变更（pre-1.0）**：`ResilienceProperties` canonical 构造组件数 13→14（兼容构造
  保留源码兼容；反射绑定按 canonical 的调用方需核对）

## effort #13 新增公共面（治理 / impl-178–180，@since 1.0.0）

**buzhou-spring-boot-starter（测试面防线，非运行时 API）**

- `ConfigBindingsMatrixTest`（93 键绑定矩阵；新键必须登记）
- `ApiSurfaceSnapshotTest` + `docs/api-surface.snapshot.txt`（466 类型黄金快照）
- **配置键破坏性改名（pre-1.0，原键本就静默无效——修复性改名）**：
  `buzhou.runaway.per-turn.max-wall-clock` → `per-turn.wall-clock`；
  `buzhou.runaway.session.*` → `runaway.per-session.*`；
  `buzhou.index.closed-retention` → `buzhou.core.index-closed-retention`
- **行为修复**：`buzhou.leak.lease-age-threshold` / `buzhou.skills.catalog-cache-ttl`
  支持 Spring 双格式时长（"5m"/"PT5M"），原仅 ISO 格式（与 metadata 文档矛盾）

## effort #14 新增公共面（spec 54 / impl-185–190，@since 1.0.0）

**buzhou-core（core.spi 包，全新）**

- `RateLimitBackend`（限流后端 SPI：tryAcquire/consume/available/capacity/
  secondsUntilAvailable/kind；策略留在 resilience，额度存取抽象到后端）

**buzhou-resilience（ratelimit 包）**

- `InMemoryRateLimitBackend`（默认内存令牌桶——原 ModelRateLimiter.TokenBucket 平移；
  单进程行为零变化）
- `ModelRateLimiter` 新增 backend 注入构造（五参重载）与 `backend()` 观测出口
  （旧构造保留 = 内存默认）
- `ResilienceModule.configure` 新增带 RateLimitBackend 尾参重载（旧签名委托）

**buzhou-store-redis**

- `RedisRateLimitBackend`（分钟固定窗 INCR/EXPIRE，AutoCloseable；多实例共享额度；
  fail-fast 故障语义 STORE_WRITE_FAILED 带修法；epoch 时基窗口键 + 模型名净化）
- `BuzhouRedisStoreAutoConfiguration` 新增 `buzhouSharedRateLimitBackend` bean
  （store.type=redis 即供；容量 env 直读 buzhou.resilience.rate-limit.*；
  destroyMethod=close）
- yml 键：**零新增**（复用 `buzhou.store.type` + `buzhou.resilience.rate-limit.*`；
  绑定矩阵防线核对通过）

## effort #15 新增公共面（spec 55 / impl-191–195，@since 1.0.0）

**buzhou-resilience（resilience.cache 包，全新两类）**

- `SemanticCacheStore`（进程内向量存储：桶内线性 cosine 最近邻 + LRU/TTL 惰性过期；
  hit/miss/evicted 计数；可注入 Clock；零向量/维度错配防御性 miss）
- `SemanticCacheAdvisor`（BaseAdvisor；order +460 = 精确缓存之后；嵌入查询/写入失败
  旁路降级 + bypassCount() 观测；终态写入边界复用 ResponseCacheAdvisor.isTerminal）
- `ResilienceProperties.SemanticCache` 参数组（顶层 record 第 15 组件；14 参兼容构造保留）
- `ResilienceModule.configure` 增 EmbeddingModel 尾参重载（旧签名委托）
- yml 键：`buzhou.resilience.semantic-cache.{enabled,similarity-threshold,max-entries,ttl}`
  （metadata 已入档 + 绑定矩阵登记——enabled=true 全路径含 stub EmbeddingModel）
- **破坏性变更（pre-1.0）**：`ResilienceProperties` canonical 构造组件数 14→15
  （兼容构造保留源码兼容；反射绑定按 canonical 的调用方需核对）

## effort #16 新增公共面（spec 56 / impl-196–198，@since 1.0.0）

- `SessionStateStore.compareAndSwap(sessionId, key, expected, update)`（default 方法：
  非原子 check-then-write；内存 compute / JDBC 条件单语句 / 池化 Redis WATCH 事务覆写真原子）
- `SessionStateHandle.compareAndSwap(key, expected, update)`（default 同上；HookEnvironment
  覆写透传 store CAS）
- `ResilienceStats`：`recordQuotaCasFallback()` / `quotaCasFallbacks()`（回退可观测）
- 类型级快照：**零新增类型**（方法级增补不入快照）；yml 键：**零新增**

## effort #17 新增公共面（spec 57 / impl-199–201，@since 1.0.0）

**buzhou-core（core.spi 包）**

- `CircuitBreakerStateBackend`（共享熔断闸后端 SPI：recordTrip/activeTrip/clear/kind +
  TripMarker(openedAt, cooldownMs, consecutiveTrips)；默认全 no-op = 进程语义零变化）

**buzhou-resilience（circuit 包）**

- `ModelCircuitBreaker` 新增 backend 注入构造（四参重载；旧构造保留 = 进程默认）
- `ResilienceModule.configure` 增 CircuitBreakerStateBackend 尾参重载（旧签名委托）
- `BuzhouResilienceAutoConfiguration`：ObjectProvider 消费共享后端；多实例告警区分
  「熔断——无共享后端」

**buzhou-store-redis**

- `RedisCircuitBreakerStateBackend`（TTL 键跳闸标记：键存活 = 全实例 OPEN、过期 =
  可探测免清理；AutoCloseable；故障降级本地语义 WARN 继续；模型名净化入键）
- `BuzhouRedisStoreAutoConfiguration` 新增 `buzhouSharedCircuitBreakerBackend` bean
  （store.type=redis 且熔断启用即供；destroyMethod=close）
- 类型级快照：+2（CircuitBreakerStateBackend / RedisCircuitBreakerStateBackend）；
  yml 键：**零新增**（复用 `buzhou.store.type` + `buzhou.resilience.circuit.enabled`）

## effort #18 新增公共面（spec 58 / impl-202–203，@since 1.0.0）

- `SessionStateStore.countByPrefix(sessionId, prefix)`（default 方法：scanByPrefix().size()
  兼容第三方；JDBC COUNT(*) 下推 / Redis 键集侧计数 / 内存键迭代覆写）
- 类型级快照：**零新增类型**；yml 键：**零新增**
- `RedisSync.batchHgetAll`（包内：共享连接 async 流水线批量 HGETALL；事务绑定线程退化逐键）

## effort #19 新增公共面（spec 59 / impl-204–205，@since 1.0.0）

**buzhou-skills**

- `SemanticSkillRanker`（目录语义排序：cosine 降序 + 原序稳定并列；技能向量缓存
  name 键 + 文本变更失效；嵌入失败回退原序 + bypassCount() 观测）
- `SkillCatalogRendererImpl` 四参构造（ranker + catalogMaxEntries；null ranker = 旧行为）
- `SkillModule.Builder`：`semanticRankingEnabled` / `embeddingModel`（enabled 无 bean
  → build() fail-fast 带修法）
- `SkillCatalogRenderer#renderCatalog(sessionId, queryHint)`（core default 方法——旧签名委托）
- yml 键：`buzhou.skills.semantic-ranking.enabled`（默认 false；metadata + 绑定矩阵
  enabled=true 全路径登记）
- 类型级快照：+1（SemanticSkillRanker）

## effort #20 新增公共面（spec 60 / impl-206，@since 1.0.0）

**buzhou-core（session 包）**

- `ObservabilityJsonlExporter`（观测 OLAP JSONL 导出：单会话/单类/全量分页驱动；
  一行一 JSON 对象、字段序稳定、duration_ms 派生、换行转义、坏值列降级 + skipped 计数；
  `JsonlExportResult(sessions, spans, events, skipped)` 返回面）
- 类型级快照：+1；yml 键：**零新增**（纯新增只读出口）

## effort #21 新增公共面（spec 61 / impl-207，@since 1.0.0）

**buzhou-core（eval 包）**

- `LlmJudgeEvaluator`（LLM-as-judge：judge ChatModel + 可选 rubric + PASS/FAIL 首词
  协议解析；`JudgeProtocolException`（不可解析 → runner 记该条 error）；DeepEval/Ragas
  G-Eval 借鉴；诚实边界：判别力/抗注入归 judge 模型、CI 不强制）
- `EvalRunner` 评估器异常收敛为该条 error（不再炸整跑）
- 类型级快照：+1；yml 键：**零新增**（宿主显式构造传入 run）

## effort #22 新增公共面（spec 62 / impl-208，@since 1.0.0）

- `AtomicStateCounters`（core.internal.hook——非公开面：值形态无关的进度检测 CAS 计数
  写助手；TokenBudgetHook / RunawayHook / SessionQuotaHook 三处计数统一）
- 类型级快照：**零新增**（internal 包不入公共面）；yml 键：**零新增**

## effort #23 新增公共面（spec 63 / impl-209，@since 1.0.0）

**buzhou-core（eval 包）**

- `PairwiseJudge`（成对 A/B 对比：双向评判消位置偏差——两方向同赢家才裁，翻转判
  position-bias TIE；协议失败 protocol TIE；`PairwiseVerdict(winner, reason)`；Ragas
  pairwise / Chatbot Arena 借鉴）
- 类型级快照：+1（外部类；内部枚举/record 随类不入）；yml 键：**零新增**

## effort #24 新增公共面（spec 64 / impl-210，@since 1.0.0）

**buzhou-resilience（fallback 包）**

- `FallbackLatencyTracker`（备模型延迟 EMA 追踪：α=0.3、未知取已知中位数中性、
  稳定排序视图；LiteLLM latency-based routing 借鉴）
- `FallbackChain` 3 参构造（tracker 注入）与 `latencyTracker()` 出口；`models()`
  排序视图
- `ResilienceAdvisor` 12 参构造（tracker 计时接线：备模型/金丝雀三处调用）
- `ResilienceProperties.Fallback` 第 5 组件 `latencyAware`（4 参兼容构造保留）
- yml 键：`buzhou.resilience.fallback.latency-aware`（默认 false；metadata + 矩阵
  enabled=true 全路径）
- **破坏性变更（pre-1.0）**：`Fallback` canonical 构造组件数 4→5（兼容构造保留源码
  兼容；反射绑定按 canonical 的调用方需核对）
- 类型级快照：+1（FallbackLatencyTracker）

## effort #25 新增公共面（spec 65 / impl-211，@since 1.0.0）

**buzhou-resilience（budget 包）**

- `AgentCostLedgerHook`（agent 级成本归集：afterModel usage→agentName 台账；合成会话
  `__buzhou.cost__`；CAS 原子跨实例；`query(store)` per-agent 行；LiteLLM spend
  tracking 借鉴；显式挂载不自动装配）
- 类型级快照：+1；yml 键：**零新增**

## effort #26 新增公共面（spec 66 / impl-212，@since 1.0.0）

- `InjectionViewProcessor.setPrefixStableInjection(boolean)`（前缀稳定注入序开关：
  catalog→summary→facts 稳定块前置；默认 false 保持 spec 04 口径）
- 类型级快照：**零新增**；yml 键：`buzhou.memory.prefix-stable-injection`（默认 false；
  metadata + 矩阵 env-read 登记）

## effort #27 新增公共面（spec 67 / impl-213，@since 1.0.0）

- `ObservabilityJsonlExporter.exportAllSince(Writer, Instant)`（增量水位导出：
  lastActivityAt 过滤 + waterline 返回；空结果水位不变；Langfuse cursor 语义）
- `JsonlExportResult` 第 5 组件 `waterline`（4 参兼容构造保留；全量导出 = null）
- 类型级快照：**零新增**；yml 键：**零新增**

## effort #28 新增公共面（spec 68 / impl-214，@since 1.0.0）

- `EvalRunner.run(dataset, evaluator, parallelism)` 三参重载（虚拟线程并行 + 项序
  聚合 + clamp 1..32；LangSmith/DeepEval 并行评估借鉴；默认路径零变化）
- 类型级快照：**零新增**；yml 键：**零新增**（API 参数非配置）

## effort #29 新增公共面（spec 69 / impl-215，@since 1.0.0）

- `RunRecoveryService.autoResumeAll()`（崩溃自愈 watchdog：枚举 RUNNING 逐一续跑，
  steal=false 租约门跳过；`AutoResumeResult(resumed, leaseHeld, failed)` 三态计数；
  失败 per-run 隔离；Temporal crash-watchdog 借鉴）
- `BuzhouCoreAutoConfiguration` 新增 `buzhouCrashResumeWatchdog` SmartLifecycle bean
  （opt-in `buzhou.recovery.auto-resume=true`，默认关）
- yml 键：`buzhou.recovery.auto-resume`（默认 false；metadata + 矩阵 env-read 登记）
- 类型级快照：**零新增**

## effort #30 新增公共面（spec 70 / impl-216，@since 1.0.0）

- `InjectionViewProcessor.setBoundaryCompactBacklog(int)`（边界机会压缩：积压 ≥ N
  提前走增量摘要路径；Letta 自然边界压缩的 Completed-Turn 代理；默认 0=关）
- yml 键：`buzhou.memory.boundary-compact-backlog`（默认 0；metadata + 矩阵 env-read）
- 类型级快照：**零新增**

## effort #31 新增公共面（spec 71 / impl-217，@since 1.0.0）

**buzhou-core（eval 包）**

- `PairwiseEvalRunner`（A/B 成对评估：双 runtime 逐项执行 + PairwiseJudge 双向裁定 +
  胜率汇总（error 不入分母）+ 并行 clamp；`PairwiseItemResult` / `PairwiseSummary` /
  `PairwiseEvalResult` 返回面；Ragas pairwise eval / LiteLLM model-compare 借鉴）
- 类型级快照：+1；yml 键：**零新增**

## effort #32 新增公共面（spec 72 / impl-218，@since 1.0.0）

**buzhou-core（eval 包）**

- `SessionTrajectoryImporter`（会话轨迹→评估数据集回流：完整轮入集 + 溯源去重 +
  三态计数；LangSmith session-to-dataset 借鉴；golden 筛选归调用方）
- 类型级快照：+1；yml 键：**零新增**

## effort #33 新增公共面（spec 73 / impl-219，@since 1.0.0）

- `SkillSearchTool` 3 参构造（SemanticSkillRanker 注入：命中语义排序 + 零命中近邻
  提示 3 条；与目录注入共享 ranker/向量缓存；无 ranker 行为零变化）
- 类型级快照：**零新增**；yml 键：**零新增**（复用 semantic-ranking.enabled）

## effort #34 新增公共面（spec 74 / impl-220，@since 1.0.0）

- `PairwiseEvalRunner` 3 参构造（SessionStateStore 落盘：`ab.run.<runId>` 记录）与
  `abRuns(store, dataset?)` 摘要查询（startedAt 倒序；`AbRunSummary` 行）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #36 新增公共面（spec 75 / impl-222，@since 1.0.0）

- `PairwiseEvalRunner.compare` 完成事件 `ab.run.completed`（eval.run.completed 家族
  扩展，LangSmith run 事件面借鉴；total>0 门；与落盘正交）
- 类型级快照：**零新增**（行为面）；yml 键：**零新增**

## effort #37 新增公共面（spec 76 / impl-223，@since 1.0.0）

- `PairwiseEvalRunner.abRun(store, runId)` 单 run 明细回读（`Optional<
  PairwiseEvalResult>`，verdict 面；LangSmith run detail API 借鉴；与 abRuns 共用
  `mapToResult` 解码底座）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #38 新增公共面（spec 77 / impl-224，@since 1.0.0）

- `EvalRunRegistry`（活跃评估 run 注册表：eval/ab 两 kind 在飞计数 + gauge
  `buzhou.eval.runs.active`（tag kind）；runId 幂等 + Registration close 幂等；
  LangSmith active-runs 观测面借鉴；BuzhouMetricsHolder 同款全局旋钮模式）
- 类型级快照：+1；yml 键：**零新增**

## effort #39 新增公共面（spec 78 / impl-225，@since 1.0.0）

- `SessionStateStore.scanByKeyRange(sessionId, prefix, fromKeyInclusive,
  toKeyExclusive, limit)`（键序区间扫描：字典序升序 + 含界下界 + 排他上界 + limit
  截断；JDBC ORDER BY/LIMIT 下推、内存键迭代覆写、默认排序兜底；Kafka log 有序读
  借鉴——「时间编进键」结构的公共底座）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #40 新增公共面（spec 79 / impl-226，@since 1.0.0）

- `WebhookOutbox` due-time 索引（键 `due.<零垫 nextAttemptAt>.<eventId>` 双写 +
  `due()` 键序区间读最早到期 + 孤儿/陈旧自愈 + 构造期回填；Kafka log+index 借鉴；
  spec 78 scanByKeyRange 的首个消费方——退避积压不再放大调度读）
- 类型级快照：**零新增**（内部结构）；yml 键：**零新增**

## effort #41 新增公共面（spec 80 / impl-227，@since 1.0.0）

- `EvalGate.enforce(dataset, evaluator, threshold[, parallelism])` → `GateResult`
  （评估回归门：passRate ≥ 阈值判过 + error 计入分母从严 + 失败项预览截 10 条 +
  CI 单行 summary；Promptfoo eval CI gate / LangSmith eval-as-gate 借鉴；执行复用
  EvalRunner 管线）
- 类型级快照：+1；yml 键：**零新增**

## effort #42 新增公共面（spec 81 / impl-228，@since 1.0.0）

- `EvalRunDiff.diff(base, head)` 两 run 逐项对比（四态迁移 REGRESSION/FIX/
  STABLE_PASS/STABLE_FAIL + 单侧项（数据集漂移）单独计数 + netDelta + 项序确定；
  LangSmith run compare 借鉴；纯函数不触 store；`runOf` 便捷构造）
- 类型级快照：+1；yml 键：**零新增**

## effort #43 新增公共面（spec 82 / impl-229，@since 1.0.0）

- `EvalDatasetStore.fingerprint(name)`（数据集内容指纹 SHA-256——内容寻址名无关，
  LangSmith dataset versioning 借鉴）+ `EvalRunResult.datasetFingerprint`（run 执行
  时刻指纹入档，9 参旧构造兼容）+ `EvalRunDiff.DiffResult.datasetDrift`（就地改项
  型漂移显形——单侧项只显形增删）
- 类型级快照：**零新增**（字段级）；yml 键：**零新增**

## effort #44 新增公共面（spec 83 / impl-230，@since 1.0.0）

- `ErrorSignatures`（错误签名聚类：异常简名+归一化首行（数字/十六进制折叠）成
  有界族，256 条封顶折 `<kind>:__overflow__`，top(n)/snapshot() 供看板与健康面；
  Sentry fingerprint 借鉴；进程内有界 Map 不进 micrometer tag；工具错误路径已接线）
- 类型级快照：+1；yml 键：**零新增**

## effort #45 新增公共面（spec 84 / impl-231，@since 1.0.0）

- `AgentBulkhead`（agent 并发 Turn 隔离舱：per-agent 信号量上限 + NOOP 零开销默认 +
  chat/chatForEntity/stream 三入口接线（stream 名额横跨流生命周期）+ QUOTA_EXCEEDED
  fail-fast/超时两档 + `buzhou.bulkhead.enabled/agents/acquire-timeout` 三键默认关；
  resilience4j Bulkhead 借鉴——spawn 闸限会话数、本舱限在飞 Turn 数，正交）
- 类型级快照：+1；yml 键：+3（buzhou.bulkhead.*，默认关）

## effort #46 新增公共面（spec 85 / impl-232，@since 1.0.0）

- `ErrorSignaturesHealth`（`/actuator/buzhou` 快照的 error-signatures 段：恒 UP +
  top-5 错误族有界详情 + distinct 数；#44 fog 毕业生——进程内 top 表接健康面）
- 类型级快照：+1；yml 键：**零新增**

## effort #47 新增公共面（spec 86 / impl-233，@since 1.0.0）

- `guard/pii` 三件套（Presidio 规则式子集借鉴）：`PiiType`（5 型）+
  `PiiDetector`（GB 11643 校验位/Luhn/IPv4 段验证收窄误报 + 重叠去重）+
  `PiiRedactionHook`（afterTool `[PII:TYPE]` 占位符改写，order 70 先于 spotlight、
  幂等 + 计数器 tag type）；装配 `GuardModule.builder().piiRedaction(types?)` /
  `buzhou.guard.pii.enabled`（默认关）+ `types`
- 类型级快照：+3；yml 键：+2（buzhou.guard.pii.enabled/types，默认关）

## effort #48 新增公共面（spec 87 / impl-234，@since 1.0.0）

- `RagasEvaluators`（Ragas 系数值评估器：faithfulness 断言支持率（幻觉面）+
  answerRelevancy 针对性 0-10（跑题面）；S x/y 协议 + clamp + 协议失败走 error
  收敛 + 分母 0 从严；与二值 LlmJudgeEvaluator 互补的连续分面）
- 类型级快照：+1；yml 键：**零新增**

## effort #49 新增公共面（spec 88 / impl-235，@since 1.0.0）

- `EvalRunJsonlExporter`（eval run OLAP JSONL 导出：item+summary 行 + 汇总列反规范
  化（单表免 join）+ 指纹列 + 诚实零行；与观测导出 spec 60/67 同族——质量-行为
  联合分析补齐）
- 类型级快照：+1；yml 键：**零新增**

## effort #50 新增公共面（spec 89 / impl-236，@since 1.0.0）

- `RagasEvaluators.gEval(judge, dimension, rubric[, threshold])`（G-Eval 自定义维度
  打分：维度名进 detail 前缀（OLAP 分组锚点）+ BOTH 参照系（输入与黄金答案齐进
  prompt）+ 空维度名 fail-fast；DeepEval G-Eval 借鉴——评分标准是数据不是代码）
- 类型级快照：**零新增**（方法级 + Reference 枚举内部化）；yml 键：**零新增**

## effort #51 新增公共面（spec 90 / impl-237，@since 1.0.0）

- `memory/compact` 漂移双件套：`SemanticDriftDetector`（函数接口——词面/嵌入/
  模型实现自由）+ `LexicalDriftDetector`（字符 bigram Jaccard 默认实现，阈值 0.15
  保守档）；InjectionViewProcessor 边界压缩并联 driftTrigger（与积压判据同管线）；
  MemoryModule 键 `buzhou.memory.semantic-drift`（默认关）+ `-threshold`；
  Letta 语义触发压缩借鉴——spec 70 双信号化（计数+漂移）
- 类型级快照：+2；yml 键：+2（buzhou.memory.semantic-drift/-threshold，默认关）

## effort #52 新增公共面（spec 91 / impl-238，@since 1.0.0）

- `ConfigDoctor`（配置体检：classpath metadata json 聚合键宇宙 + 未知键 WARN 近邻
  建议（编辑距离 ≤2）+ 值域越界 ERROR（Boolean 严格白名单）+ 有界报告 + 单行
  summary + `examine(Environment)` 聚合入口；`buzhou.config-doctor.enabled` 默认关
  就绪事件日志一次；Spring Shell doctor 借鉴——#52 插曲产品化）
- 类型级快照：+1；yml 键：+1（buzhou.config-doctor.enabled，默认关）

## effort #53 新增公共面（spec 92 / impl-239，@since 1.0.0）

- `BulkheadHealth`（/actuator/buzhou 的 bulkhead 段：未配置 UNKNOWN + disabled
  详情；配置后 UP + per-agent inFlight/limit 有界详情 16 条截断；#84 fog 毕业生）
  + `AgentBulkhead.configuredAgents()`（agent → limit 只读视图）
- 类型级快照：+1；yml 键：**零新增**

## effort #54 新增公共面（spec 93 / impl-240，@since 1.0.0）

- `PairwiseEvalResult.datasetFingerprint` / `AbRunSummary.datasetFingerprint`（A/B
  run 执行时刻数据集指纹入档——与 spec 82 同语义，7 参 record + 6 参旧构造兼容，
  null 不写键；数据集演进后 A/B 结论适用性可验）
- 类型级快照：**零新增**（字段级）；yml 键：**零新增**

## effort #55 新增公共面（spec 94 / impl-241，@since 1.0.0）

- `AbRunJsonlExporter`（A/B run OLAP JSONL 导出静态面：verdict item 行 + summary 行
  + 汇总列反规范化 + 指纹列 + 未知 runId 诚实零行 + exportAll 倒序；spec 88 的
  AB 面同构——四导出面族补齐）
- 类型级快照：+1；yml 键：**零新增**

## effort #56 新增公共面（spec 95 / impl-242，@since 1.0.0）

- `CompactionListener.onSummaryFolded(sessionId, summary, trigger)`（摘要折入通知
  default 方法——trigger ∈ budget/backlog/drift 溯源；lambda 兼容）+ MemoryModule
  装配双写 `memory.summary.folded` 事件（payload: trigger/generation/coversUpToTurn；
  spec 90 fog 收口——压缩观测双事件族补齐）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #57 新增公共面（spec 96 / impl-243，@since 1.0.0）

- `WebhookOutboxAudit`（outbox due 索引一致性审计：孤儿/陈旧/缺失三类失真只读
  对账 + 计数样本 + `repair` 按项清/补（默认全 false safe-by-default）+ 纯静态
  不触写路径；StoreFsck 同思想第三域——spec 79 fog 收口，投递停摆提前可见）
- 类型级快照：+1；yml 键：**零新增**

## effort #58 新增公共面（spec 97 / impl-244，@since 1.0.0）

- `SessionArchiver`（会话归档冷层：三槽快照落 `__buzhou.archive__` 合成会话 +
  SessionCleaner 级联删除 + restore 原键原值回放 + archived() 清单 + 编码失败
  fail-fast 不删 + Instant SimpleModule 编解码无 jsr310 依赖；删除前置安全网——
  #35 fog 收口）
- 类型级快照：+1；yml 键：**零新增**

## effort #59 新增公共面（spec 98 / impl-245，@since 1.0.0）

- `RedisSessionStateStore.scanByKeyRange` 覆写（SMEMBERS 键侧过滤 + TreeMap 排序
  截断 + 命中键 batchHgetAll 一次往返——spec 78 三栈下推补齐 Redis 侧；#39 fog
  收口；jedis-mock 内嵌单测）
- 类型级快照：**零新增**（覆写级）；yml 键：**零新增**

## effort #61 新增公共面（spec 99 / impl-246，@since 1.0.0）

- 折入速率指标：counter `buzhou.memory.summary.folded` / `fold-skipped`
  （tag trigger=budget/backlog/drift 有界——成功与熔断跳过双面；spec 95 fog 收口）
- 类型级快照：**零新增**（指标级）；yml 键：**零新增**

## effort #62 新增公共面（spec 100 / impl-247，@since 1.0.0）

- `EvalDatasetStore.snapshotDataset(source, target)`（数据集快照副本：原 id 复制
  指纹与源一致 + target 不可覆盖 + 快照可续 addItem 分叉——冻结版本底座；
  LangSmith dataset versioning 借鉴，spec 82 fog 收口）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #63 新增公共面（spec 101 / impl-248，@since 1.0.0）

- `PairwiseGate.enforce(...)` → `AbGateResult`（A/B 胜率门：winRateA ≥ 阈值判定 +
  逐项预览截 10 + CI 单行 summary + error 不入分母 spec 71 口径；Promptfoo
  model-compare 借鉴——与 EvalGate 组成双门族，spec 80 fog 收口）
- 类型级快照：+1；yml 键：**零新增**

## effort #64 新增公共面（spec 102 / impl-249，@since 1.0.0）

- `ArchiveHealth`（/actuator/buzhou 的 session-archive 段：恒 UP + 在册数
  countByPrefix 下推）+ `SessionArchiver.ARCHIVE_PREFIX` 公共化（spec 97 fog 前半场）
- 类型级快照：+1；yml 键：**零新增**

## effort #65 新增公共面（spec 103 / impl-250，@since 1.0.0）

- `SessionArchiver.purgeExpired(ttl, now)`（归档 TTL 清理：到期删/损坏跳过不阻断/
  ttl≤0 显式全清/幂等；now 外注同 SessionHistoryPolicy 签名纪律；S3 lifecycle
  借鉴——spec 102 fog 后半场，归档治理闭环「可见→治理」补齐）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #66 新增公共面（spec 104 / impl-251，@since 1.0.0）

- 模型失败签名接线（DefaultAgentSession 直调/守护双路径旁路 record("model", …)，
  抛出语义零变化——ErrorSignatures tool+model 双族补齐，spec 83 fog 收口）+
  ArchiveHealth 装配修正（ObjectProvider + store 缺席 UNKNOWN-disabled，不抢
  启动 store 校验报错优先级）
- 类型级快照：**零新增**（接线级）；yml 键：**零新增**

## effort #67 新增公共面（spec 105 / impl-252，@since 1.0.0）

- `WebhookEventForwarder.setIncludeTypes(...)` + `buzhou.webhook.include-types`
  （订阅类型过滤：命中才入队、被滤不占 outbox 容量 + `buzhou.webhook.filtered`
  计数；空/缺省 = 全投递零变化；GitHub/Stripe webhook 订阅面借鉴）
- 类型级快照：**零新增**（方法级）；yml 键：+1（buzhou.webhook.include-types，默认全投递）

## effort #68 新增公共面（spec 106 / impl-253，@since 1.0.0）

- `PiiInputRedactionHook`（用户输入 PII 脱敏：beforeTurn replaceInput 占位符化 +
  幂等 + 类型集与输出侧共用 + `buzhou.guard.pii.input-redaction` 独立开关默认关；
  spec 86 fog 收口——输入/输出双侧防线闭环）
- 类型级快照：+1；yml 键：+1（buzhou.guard.pii.input-redaction，默认关）

## effort #69 新增公共面（spec 107 / impl-254，@since 1.0.0）

- `ConfigDoctorHealth`（/actuator/buzhou 的 config-doctor 段：就绪一次体检缓存 +
  UNKNOWN(pending)→UP + errors/warnings/checkedKeys 有界详情；listener+health
  复合 bean 替换原装配——spec 91 fog 收口）
- 类型级快照：+1；yml 键：**零新增**（复用 buzhou.config-doctor.enabled）

## effort #70 新增公共面（spec 108 / impl-255，@since 1.0.0）

- 工具调用时长 timer：`buzhou.tool.duration`（tag outcome=ok|failed——delegate
  调用本体 nanoTime 计时；既有计数/错误反馈/签名通道零变化；P95 慢工具告警底座）
- 类型级快照：**零新增**（指标级）；yml 键：**零新增**

## effort #71 新增公共面（spec 109 / impl-256，@since 1.0.0）

- `ObservabilityJsonlExporter.exportAllGzip / exportAllSinceGzip`（gzip 压缩导出：
  GZIP+UTF-8 Writer 复用既有管线，解压与明文逐字节一致；水位/计数语义不变；
  spec 88 fog 收口——归档/跨网体积降一个量级）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #72 新增公共面（spec 110 / impl-257，@since 1.0.0）

- 技能目录注入遥测：counter `buzhou.skills.catalog-injected` +
  `catalog-overflow`（tag outcome=truncated|fit——截断率即 catalog-max-entries
  调优信号；双注入路径同源计数；空目录零计数）
- 类型级快照：**零新增**（指标级）；yml 键：**零新增**

## effort #73 新增公共面（spec 111 / impl-258，@since 1.0.0）

- 评估 run 时长 timer：`buzhou.eval.run.duration` / `buzhou.eval.ab-run.duration`
  （完成点计时复用既有时间值——数据集规模感知的时长回归信号；LangSmith run
  latency 借鉴）
- 类型级快照：**零新增**（指标级）；yml 键：**零新增**

## effort #74 新增公共面（spec 112 / impl-259，@since 1.0.0）

- `ErrorSignaturesJsonl.export(registry, Writer)`（错误签名 OLAP JSONL 导出：
  snapshot 全量一行一 JSON count 降序——错误族趋势进数仓量化治理效果；
  spec 83 fog 收口，导出五族补齐）
- 类型级快照：+1；yml 键：**零新增**

## effort #75 新增公共面（spec 113 / impl-260，@since 1.0.0）

- `EvalQueryService.runsOfVersion(fingerprint)`（按数据集内容版本跨名聚合查 run
  + `EvalRunSummary.datasetFingerprint` 列（8 参旧构造兼容）——spec 82/100 组合
  收口：snapshotDataset → runsOfVersion → EvalRunDiff 版本回归全链路）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #76 新增公共面（spec 114 / impl-261，@since 1.0.0）

- `PairwiseEvalRunner.abRunsOfVersion(store, fingerprint)`（AB run 按数据集内容
  版本聚合查询——spec 113 的 AB 面同构；同基线版本对比集零胶水）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #77 新增公共面（spec 115 / impl-262，@since 1.0.0）

- ConfigDoctor v2 跨键规则表（四规则：NOOP 空转 / 孤儿依赖静默空转 ×2 / 开关联
  矛盾——单键合法但组合矛盾的配置启动期点名；spec 91 fog 收口，IDE inspections
  借鉴）
- 类型级快照：**零新增**（规则级）；yml 键：**零新增**

## effort #78 新增公共面（spec 116 / impl-263，@since 1.0.0）

- skill_search 遥测：counter `buzhou.skills.search`（tag outcome=hit|miss|
  miss-semantic 三值——命中率即技能可发现性信号，语义救回单列；文案/排序零变化）
- 类型级快照：**零新增**（指标级）；yml 键：**零新增**

## effort #79 新增公共面（spec 117 / impl-264，@since 1.0.0）

- `AgentBulkhead.topRejections(n)`（per-agent 拒绝计数进程内有界表：256 封顶折
  overflow + 稳定排序 + NOOP agent 零计数；BulkheadHealth 增 topRejected 3 条
  ——限流风暴一屏定位，spec 84 fog 收口）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #80 新增公共面（spec 118 / impl-265，@since 1.0.0）

- `CustomPiiRules`（自定义 PII 规则：命名正则 [A-Z0-9_]{2,32} fail-fast + 命中
  [PII:NAME] 占位符与内置同形态 + 叠加不短路；`PiiRedactionHook` 3 参构造——
  Presidio PatternRecognizer 对应物，spec 86 fog 收口）
- 类型级快照：+1；yml 键：**零新增**（编程面——yml 声明式 fog 记账）

## effort #81 新增公共面（spec 119 / impl-266，@since 1.0.0）

- `ObservabilityJsonlExporter.exportSessionGzip`（单会话 gzip 导出——工单附件/
  事故取证场景；与明文逐字节一致；gzip 三入口族完整：全量/增量/单会话）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #82 新增公共面（spec 120 / impl-267，@since 1.0.0）

- `SessionArchiver.archivedDetailed()`（归档详情：sessionId/archivedAt/messageCount/
  stateCount 倒序——合规审计零解析；损坏归档 -1 占位行可见不静默）
- 类型级快照：**零新增**（方法级 + 内部 record）；yml 键：**零新增**

## effort #83 新增公共面（spec 121 / impl-268，@since 1.0.0）

- `ErrorSignatures.reset()`（窗口化清零：export → reset 循环 = 每窗口一份 JSONL、
  进程内表永有界——spec 112 fog 收口，错误族时序管线补齐）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #86 A 侧新增公共面（spec 122 / impl-272，@since 1.0.0）

- `SuperstepBatch.runAll`（concurrent 包事务性并行批原语：完成序感知首败即中止
  在途 + 部分结果不可见 + 每任务去向入异常；`SUPERSTEP_FAILED` 新错误码——
  与 B 侧 harness 原子前检同轮 A/B 分工，MAP 登记）
- 类型级快照：+1（concurrent 包静态工具类）；yml 键：**零新增**

## effort #88 新增公共面（spec 124 / impl-274，@since 1.0.0）

- `budget/VirtualKeys`（虚拟 key 配额：per-key token 硬顶 + AtomicLong CAS 原子
  扣减越限整体拒绝 + usage/topUsage 井读 + reset 窗口清零——LiteLLM virtual-key
  budgets 借鉴，未注册 key 直通）
- 类型级快照：+1（budget 包 final 类 + KeyUsage record）；yml 键：**零新增**

## effort #89 新增公共面（spec 125 / impl-275，@since 1.0.0）

- `FileSandbox.forTenant(root, tenant)`（租户隔离沙箱：tenants/<tenant> 子根 +
  零追加白名单严格收窄 + id 白名单 [a-z0-9][a-z0-9-]{0,31} fail-fast——跨租户
  遍历复用既有边界检查拒绝，Milvus partition-key/chroot-per-tenant 借鉴）
- 类型级快照：**零新增**（静态工厂方法级）；yml 键：**零新增**

## effort #90 新增公共面（spec 126 / impl-276，@since 1.0.0）

- `cache/PromptPrefixCache`（提示前缀缓存：规范形 sha256 键有界 LRU + 命中续命
  + 逐出诚实计数 + getOrLoad 惰性装载 + Stats 四计数 hitRate——vLLM/SGLang
  radix prefix-cache 借鉴，不猜语义相似与向量面正交）
- 类型级快照：+1（新 cache 包 final 类 + Stats record）；yml 键：**零新增**

## effort #92 新增公共面（spec 130 / impl-277，@since 1.0.0）

- `retention/ArchivePurgeJob`（归档 TTL 定时清理：SmartLifecycle 单线程
  scheduleWithFixedDelay + purgeOnce 手动面 + listener 删除数可观测 0 也通知 +
  单轮异常不杀调度线程——S3 lifecycle 借鉴，spec 103 fog 收口）
- `config/BuzhouArchiveProperties` + autoconfig：`SessionArchiver` 兜底 bean +
  purge job（purge-enabled 默认关）
- 类型级快照：+2；yml 键：+3（buzhou.session-archive.purge-enabled/purge-ttl/
  purge-interval）

## effort #111 新增公共面（spec 132 / impl-278，@since 1.0.0）

- `metrics/TagCardinalityGuard`（tag 基数守卫：装饰任意 BuzhouMetrics——
  per-(名,键) 去重值集 64 封顶越限折 __overflow__ 样本不丢 + 指标名空间 512
  满则新名全折 + folds() 守卫面 + 畸形键值透传不放大故障，Loki cardinality
  limit 借鉴——「tag 有界」从纪律变机制）
- 类型级快照：+1；yml 键：**零新增**

## effort #112 新增公共面（spec 134 / impl-279，@since 1.0.0）

- `eval/DatasetExpectations`（数据集期望套件：四内置期望 + named 自定义行级 +
  只读 validate 带行号发现样本封顶 10 + 共 N 处诚实计数 + 单行 summary——
  Great Expectations 借鉴，脏数据 run 前 fail-fast）
- 类型级快照：+1（+Finding/Result/Expectation 内部 record）；yml 键：**零新增**

## effort #113 新增公共面（spec 136 / impl-280，@since 1.0.0）

- `ObservabilityJsonlExporter.exportAllSampled`（尾采样导出：错误/慢会话 100%
  保留 + 健康快会话确定性哈希比率留样——同 id 重导同判定；会话粒度完整叙事；
  TailSamplingPolicy + SampledExportResult kept/notSampled 分列 + 单行 summary，
  OTel tail sampling 借鉴）
- 类型级快照：**零新增**（方法级 + 嵌套 record）；yml 键：**零新增**

## effort #114 新增公共面（spec 138 / impl-281，@since 1.0.0）

- `runaway/TurnHeartbeat`（轮次心跳：注册制在飞表 + beat 进展打点 + stalled
  候选制检测 quiet 降序最长停滞优先 + stalled-detected 计数——「活着但不动」
  卡死面可见，Temporal Activity heartbeat 借鉴）
- 类型级快照：+1（+Stalled record）；yml 键：**零新增**

## effort #115 新增公共面（spec 140 / impl-282，@since 1.0.0）

- `skill/SkillUsageStats`（技能使用统计：LoadSkillTool 成功打点 + topUsed
  排行稳定排序 + unused 零使用清单治理证据面 + reset 窗口清零 + 1024 封顶折
  __overflow__——Backstage catalog score 借鉴）
- 类型级快照：+1（buzhou-skills + SkillUsage record）；yml 键：**零新增**

## effort #116 新增公共面（spec 142 / impl-283，@since 1.0.0）

- `ConfigDoctorHealth(env, freshnessTtl)` + `reexamine()`（体检陈旧度：报告
  超 TTL 转 UNKNOWN(stale)——旧快照不冒充现在；手动刷新面恒可用；默认无 TTL
  零变化；details 增 examinedAt/freshnessTtlMs/stale，Consul TTL check 借鉴）
- 类型级快照：**零新增**（构造器 + 方法级）；yml 键：**零新增**

## effort #117 新增公共面（spec 144 / impl-284，@since 1.0.0）

- `guard/pii/PiiHitStats`（PII 命中统计：内置类型 + 自定义规则名统一排行 +
  PiiRedactionHook 双点接线自定义补盲 + 自定义名 64 封顶折 __overflow__ +
  reset 窗口清零——Presidio anonymizer 统计口径借鉴）
- 类型级快照：+1（buzhou-guard + Hit record）；yml 键：**零新增**

## effort #118 新增公共面（spec 146 / impl-285，@since 1.0.0）

- `ObservabilityJsonlExporter.exportManifest`（导出清单：一行一会话六列
  id/首末活动/轮次/span/event 计数——eventCount 现算与数据体互核，git pack
  索引借鉴；列序稳定行序无承诺）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #119 新增公共面（spec 148 / impl-286，@since 1.0.0）

- `TokenBudgetHook(props, model, store, virtualKeys, virtualKey)` + 
  `EVENT_KEY_HARD_STOP`（key 级预算闸：afterModel 跨会话扣减 + 越限即刻观测
  事件 + beforeModel 耗尽拦截模型零调用 + `VirtualKeys.isExhausted` 越限锁定
  至 reset——LiteLLM virtual-key 闸位语义；null = 零变化）
- 类型级快照：**零新增**（构造器 + 方法级）；yml 键：**零新增**（编程面）

## effort #120 新增公共面（spec 150 / impl-287，@since 1.0.0）

- `EvalRunner.setExpectations`（run 前期望门禁：脏数据集 fail-fast 模型零
  调用零 token 成本——message 带 summary + 前三条发现；null = 零变化，
  spec 134 账本的闸位接线）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #121 新增公共面（spec 152 / impl-288，@since 1.0.0）

- `runaway/TurnHeartbeatHook`（心跳接线：轮次起止自动注册/清除 + 模型与工具
  四点自动 beat + order 50 先留痕后裁决 + 永续 CONTINUE 观测不干预 +
  heartbeat() 共享视图——spec 138 的接线面）
- 类型级快照：+1；yml 键：**零新增**

## effort #122 新增公共面（spec 154 / impl-289，@since 1.0.0）

- `health/VirtualKeysHealth`（虚拟 key 健康面：恒 UP 观测不裁决 + distinct/
  exhausted 全量计数 + top-8 用量行 used/limit/exhausted 行内标记 +
  @ConditionalOnBean 按需装配——spec 148 key 配额的观测闭环）
- 类型级快照：+1；yml 键：**零新增**

## effort #123 新增公共面（spec 158 / impl-290，@since 1.0.0）

- `config/BuzhouVirtualKeyProperties` + autoconfig `buzhouVirtualKeys` bean
  （虚拟 key yml 装配：active-key + limits.<key> 两键即得 key 级预算闸全链——
  registry → tokenBudgetHook → VirtualKeysHealth 自动出现；bind/装配分期
  校验不带病上线）
- 类型级快照：+1；yml 键：+2（buzhou.virtual-keys.active-key/limits——后者
  Map 结构化面矩阵 SKIPPED 登记）

## effort #124 新增公共面（spec 160 / impl-291，@since 1.0.0）

- `buzhou.metrics.cardinality-guard.enabled`（tag 基数守卫 opt-in 装配：
  开 = Holder 安装面装饰 TagCardinalityGuard 全局生效；默认关零变化——
  spec 132 的装配面收口）
- 类型级快照：**零新增**；yml 键：+1

## effort #125 新增公共面（spec 162 / impl-292，@since 1.0.0）

- `runaway/TurnStallWatchdog`（停滞巡检犬：registered 全集低频轮询 + quiet
  超阈值按停滞时长降序交付 listener + 每轮重复告警诚实语义——去重归接收端 +
  空表也通知 + 单轮异常不杀调度；TurnHeartbeat 增 registered() 全集视图，
  K8s liveness probe 借鉴）
- 类型级快照：+1；yml 键：**零新增**

## effort #126 新增公共面（spec 164 / impl-293，@since 1.0.0）

- `PiiInputRedactionHook` 双点接线 PiiHitStats + `PiiHitStats
  .extractCustomRuleNames`（输入侧命中进同一张合规报表——自定义占位符提取
  上移双钩共用，内置类型名剔除防双计；spec 144 fog 收口）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #127 新增公共面（spec 166 / impl-294，@since 1.0.0）

- `guard/pii/PiiHitStatsJsonl.export`（PII 命中报表 JSONL：与 top 同序平铺
  + export→reset 每窗口一份合规报表 + 转义纪律 + 空表零行诚实——导出族
  第六员，spec 164 fog 收口）
- 类型级快照：+1；yml 键：**零新增**

## effort #128 新增公共面（spec 168 / impl-295，@since 1.0.0）

- `SkillCatalogRendererImpl` 渲染缓存 + `renderCacheStats()`（目录渲染按
  name|description 规范形 sha256 内容寻址——同目录命中复用、上架/改文案即换键
  自然失效、输出零变化；PromptPrefixCache 首个内置消费方，vLLM radix
  prefix-cache 借鉴）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #129 新增公共面（spec 170 / impl-296，@since 1.0.0）

- `skill/SkillUsageStatsJsonl.export`（技能使用报表 JSONL：与 topUsed 同序
  平铺 + export→reset 每窗口一份热度榜 + 空表零行诚实——导出族第七员，
  spec 140 fog 收口）
- 类型级快照：+1；yml 键：**零新增**

## effort #130 新增公共面（spec 172 / impl-297，@since 1.0.0）

- 归档清理三键登记补齐（additional-metadata +3 + 矩阵 Binder 面 +
  PREFIX_TO_BEAN 归属 BuzhouArchiveProperties——spec 130 漏面收口，
  T187 静默失防线闭环）
- 类型级快照：**零新增**；yml 键：**零新增**（既有键补登记）

## effort #131 新增公共面（spec 174 / impl-298，@since 1.0.0）

- `budget/ModelCostLedger`（模型成本台账：per-model micro-USD 整数累计 +
  topByCost 稳定排行 + totalMicroUsd 总数含 overflow + reset 窗口——
  WandB/Langfuse cost tracking 借鉴，只记账不拦截）
- 类型级快照：+1（+ModelCost record）；yml 键：**零新增**

## effort #131 新增公共面（spec 174 / impl-298，@since 1.0.0）

- `budget/ModelCostLedger`（模型成本台账：per-model micro-USD 整数累计 +
  topByCost 稳定排行 + totalMicroUsd 总数含 overflow + reset 窗口——
  WandB/Langfuse cost tracking 借鉴，只记账不拦截）
- 类型级快照：+1（+ModelCost record）；yml 键：**零新增**

## effort #132 新增公共面（spec 176 / impl-299，@since 1.0.0）

- `TokenBudgetHook.afterModel` 成本台账单点入账（价目换算处直入
  ModelCostLedger.global——零配置全局成本账；无价目零值在册诚实；
  只记账不拦截，spec 174 fog 收口）
- 类型级快照：**零新增**（接线级）；yml 键：**零新增**

## effort #133 新增公共面（spec 178 / impl-300，@since 1.0.0）

- `backpressure/RetryBudget`（重试预算：流量百分比毫单位连续累积 + CAS 支取
  不足即拒 + denied 风暴压制证据面 + 冷启动底数 + refill 逃逸——Finagle
  retry budget 借鉴，防重试风暴）
- 类型级快照：+1；yml 键：**零新增**

## effort #135 新增公共面（spec 182 / impl-302，@since 1.0.0）

- `retention/AdvisoryFileLock`（文件咨询锁：createNewFile 原子抢锁 + 仅持有者
  释放他者拒 + stale 陈旧判定 + forceRelease 处置——多实例单跑通用底座，
  ShedLock 借鉴，spec127/162「多实例节流」fog 起步）
- 类型级快照：+1；yml 键：**零新增**

## effort #136 新增公共面（spec 184 / impl-303，@since 1.0.0）

- `ArchivePurgeJob(…, AdvisoryFileLock)` + `SKIPPED_LOCKED`（清理接锁档：每轮
  抢锁未获跳过通知 -1「别的实例在跑」+ finally 用后即还 + IO 失败 fail-safe
  跳过；null = 零变化——spec 127 多实例节流 fog 落地）
- 类型级快照：**零新增**（构造器 + 常量）；yml 键：**零新增**（编程面）

## effort #137 新增公共面（spec 186 / impl-304，@since 1.0.0）

- `TurnStallWatchdog(…, AdvisoryFileLock)` + `skippedForLock()`（巡检犬接锁：
  未获锁零通知——null 哨兵与空表「跑过没事」严格区分 + 跳过计数证据面 +
  finally 用后即还；null = 零变化，spec 182 第二站接线）
- 类型级快照：**零新增**（构造器 + 方法级）；yml 键：**零新增**

## effort #138 新增公共面（spec 188 / impl-305，@since 1.0.0）

- `budget/ModelCostLedgerJsonl.export`（成本账单 JSONL：与 topByCost 同序 +
  双口径列 microUsd 精确/usd 6 位小数人读 + export→reset 每窗口一份 + 空表
  零行诚实——导出族第八员，spec 174 fog 收口）
- 类型级快照：+1；yml 键：**零新增**

## effort #139 新增公共面（spec 190 / impl-306，@since 1.0.0）

- `health/ModelCostHealth`（模型成本健康面：恒 UP 观测 + distinct/total 双
  口径 + top-8 烧钱行有界——台账/JSONL/健康段三面齐）
- 类型级快照：+1；yml 键：**零新增**

## effort #141 新增公共面（spec 194 / impl-308，@since 1.0.0）

- `ObservabilityJsonlExporter.exportManifestGzip`（清单 gzip 面：解压与明文
  逐字节一致——gzip 族管线合流）+ `VirtualKeys.resetAll`（整窗换窗：用量+
  耗尽态同清、限额保留）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #142 新增公共面（spec 196 / impl-309，@since 1.0.0）

- `ErrorSignatures.top(kind, n)`（按 kind 分面的错误族排行——「只看模型侧/
  工具侧」看板）+ `TurnHeartbeat.stalledSince`（单会话停滞时长查询——
  超阈 Duration/未超未注册 null，与批量 stalled 互补）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #143 新增公共面（spec 198 / impl-310，@since 1.0.0）

- `EvalRunner.setExpectations(suite, warnOnly)`（门禁宽松档：未过 WARN 带
  full detail 照跑——灰度期「看到脏但照跑」；单参严格档零变化，
  spec 150 fog 收口）
- 类型级快照：**零新增**（方法级）；yml 键：**零新增**

## effort #300–#328 新增公共面（C 会话 R1–R29 / spec 300–328 / impl-323–351，@since 1.0.0）

> 一次性入档 38 型（B 尾巴 + C 会话 R1–R29 攒量）；快照机同步修跨平台
> （File.pathSeparator + `\`→`/` 归一——Windows 本机首次真比对/再生，
> 此前 split(":") 在 Windows 切碎 classpath 且 `\classes` 不匹配 `/classes`，
> 门恒跳过、regenerate 写空文件）。

**buzhou-core**

- 运维弧线：`SessionDisruptionBudget` + `SessionDisruptionBudgetProperties`（318 排水预算——K8s PDB）；
  `BulkheadScalingAdvisor` + `BulkheadScalingProperties`（319 伸缩建议——K8s HPA）；
  `BuzhouConfigRefreshEvent` + `BulkheadHotReload`（320 容量热调整——Spring Cloud rebind）；
  `LeaderElector` SPI + `InMemoryLeaderElector`（331 选主——K8s leader election/etcd lease）
- 告警/观测：`AlertRuleEngine` + `BuzhouAlertProperties`（312——Grafana ruler）；
  `AlertGate`（330——Alertmanager 静默窗+抑制规则，嵌套 `Silence`/`InhibitRule`/`Silenced`）；
  `BuzhouProbes` + `BuzhouProbesEndpoint` + `BuzhouProbeProperties`（332——K8s 三探针分层，嵌套 `ProbeClass`/`Verdict`）；
  `BuzhouConfigSnapshotEndpoint`（343——生效配置自描述+密钥掩码，actuator configprops 思想）；
  `BuzhouAlertsEndpoint`（345——告警面板：312 firing 视图+330 静默/抑制聚合，Alertmanager UI 思想）；
  `BuzhouSessionsEndpoint`（346——会话面板：活跃计数+准入地板多源+cordon 态，面板三部曲之三）；
  `ErrorBudget` + `ErrorBudgetHook` + `ErrorBudgetHealth` + `ErrorBudgetProperties`（321——Google SRE burn）；
  `ExportBundle`（317——OCI artifact 合流打包）
- 演练/事故：`ChaosMonkeyHook` + `ChaosProperties`（322——Chaos Monkey）；
  `DryRunHook` + `DryRunProperties` + `DryRunPlanJsonl`（323/328——Terraform plan/apply）；
  `ToolKillSwitchHook` + `ToolKillSwitchHotReload` + `ToolKillSwitchProperties`（325——kill switch）
- 失控防护：`TurnRepetitionDetector` + `RepetitionDetectorHook` + `RepetitionProperties`（326——context rot）；
  `ToolLoopBreakerHook` + `ToolLoopProperties`（327——调用形态断路）
- 流量/共享族：`CanaryToolCallback`（324——Istio/Flagger canary）；
  `LaneStateBackend` + `BackendLanePermit`（core.exec，316 共享泳道 SPI）；
  `VirtualKeyBudgetBackend`（core.spi，315 共享配额 SPI）
- 安全：`EnvelopeCipher` + `EncryptingMessageStore` + `BuzhouMessageEncryptionProperties`
  （333——Vault transit/KMS envelope：消息静态信封加密，AAD 绑定+双钥轮换）；
  `EncryptingSummaryStore`（336——同通道扩散摘要槽，state 槽 CAS 比值面为诚实边界）

**buzhou-guard**

- `AuditChainHealth`（344——CT log/链全节点验证：审计链完整性巡检，
  断链 DOWN 定位首断点、超窗 UNKNOWN 带修法）
- 背压：`SpawnAdmissionFloor` + `ErrorBudgetPolicy` + `ErrorBudgetFreezeProperties`
  （335——Google SRE error budget policy：烧穿自动冻结低优先级 spawn）；
  `MaintenanceCordon` + `BuzhouMaintenanceProperties`（342——K8s cordon：
  维护窗/运行时按钮 cordon，地板多源合成与冻结正交）；
  `RetryBudgetHealth`（348——Finagle 预算水位：恒 UP+余量/被拦快照，背压族观测收口）；
  `TurnRateLimitHook`（嵌套 `Policy`）+ `BuzhouTurnRateLimitProperties`
  （425——nginx token bucket：beforeTurn 惰性令牌桶——burst+匀速回填、
  默认 per-session 键可插拔租户帽、超限 block 不炸轮）
- 执行脊柱：`ToolBaggage`（337——W3C Baggage/OTel：工具上下文行李，
  yml 播种+运行时 API+有界封顶）
- 治理：`ConfigDriftAuditor`（嵌套 `Change`）+ `BuzhouConfigAuditProperties`
  （414——ArgoCD drift detection：周期快照 diff+末段掩码同判定）/
  `PromptVersion` + `PromptRegistry` + `InMemoryPromptRegistry` +
  `BuzhouPromptProperties`（401——Langfuse prompt management：版本+标签
  双轴，publish 单调/latest 自动指针/晋级回滚同一动作/按版钉取/yml 幂等播种）；
  `PromptUsageStats`（嵌套 `Row`）+ `UsageTrackingPromptRegistry` +
  `PromptUsageJsonl` + `BuzhouPromptUsageProperties`
  （424——#401 扩散：Langfuse prompt analytics——三 resolve 形态命中记账
  +快照 JSONL 导出，usage-tracking=true 装饰器包注册表）
- 运维：`SessionAffinity`（415——Ketama 确定性键：sha256 亲和键+桶位纯函数）；
  `PeriodBudgetHealth`（419——#408 扩散：恒 UP 双轨进度+resetsAt 回血时刻）
- 成本：`PricingTable`（嵌套 `Price`）（417——320/340 rebind 同模式：
  刷新事件整表热载价目覆盖层+逐键 WARN diff）；
  `PeriodBudgetHook`（嵌套 `Unit`/`Pricing`）+ `BuzhouPeriodBudgetProperties`
  （408——AWS Budgets calendar：月/周/日账期双轨预算，periodTag 入键翻页即
  隐式重置）/ `SpendRateRing` + `CostForecast` + `CostForecastHealth` +
  `BuzhouCostForecastProperties`（403——AWS Budgets forecast：分钟桶速率环
  ×水平线线性外推 projectedOver，ModelCostLedger 监听缝喂数、恒 UP 预测面）
- 观测治理：`DashboardQueryService.TimeBucket` + rollups API + `/api/rollups`
  （412——M3 downsampling：epoch 对齐固定桶+空桶补齐+桶数上界；416 扩散
  TURN p50/p95/p99 exact 最近秩、空桶 null）
- 并发原语：`PriorityLane`（411——Envoy priority levels：优先级插队信号量，
  同级 FIFO+超时让位+等待快照）；`DelayedJobQueue`（嵌套 `PendingJob`）
  （413——Sidekiq delayed_jobs：one-shot 到点执行+键即幂等锚替换+异常隔离）；
  `PriorityLaneToolCallback` + `BuzhouToolLaneProperties`（嵌套 `LaneSpec`/
  `ToolBinding`）（422——#411 扩散：yml 声明泳道+per-tool 优先级即装配，
  未知泳道引用启动红；`ToolLaneRegistry.priorityLane` 命名单例）
- 记忆治理：`SharedFact` + `SharedFactStore` + `InMemorySharedFactStore`
  （410——mem0：跨会话共享事实 deny-by-default ACL，键即所有权）
- 评测：`TurnSamplerHook`（嵌套 `Policy`）+ `BuzhouEvalSamplingProperties`
  （407——Honeycomb head sampling：确定性 hash 采样入集 fail-soft）；
  `TurnErrorSampler`（嵌套 `Policy`）+ `BuzhouErrorSamplingProperties`
  （423——#407 扩散：OTel tail_sampling ERROR 全保——观察者缝采错误轮，
  error-rate-percent 默认 100、占位 [TURN-ERROR] 留人工判 golden）
- 工具治理：`ToolCatalogLinter`（嵌套 `Finding`）（420——ESLint：装配期
  三规则体检只报不改）/ `ToolResultSchemaHook` + `BuzhouToolResultSchemasProperties`
  （409——MCP outputSchema：结果契约复用入参校验器、违例转结构化反馈）/
  `DeprecatedToolCallback`（嵌套 `Deprecation`）+
  `BuzhouToolDeprecationProperties`（406——K8s API deprecation：描述前缀随
  定义下发+调用事件计数，迁移进度由 usage 说话）
- 运维：`HealthTimeline`（嵌套 `Entry`）+ `HealthTimelineRecorder` +
  `HealthTimelineJsonl` + `BuzhouTimelineEndpoint` +
  `BuzhouHealthTimelineProperties`（405——PagerDuty incident timeline：
  diff-only 变迁环+计数辨抖动+JSONL+/actuator/buzhou-timeline）
- B 尾巴补档：`RetryBudgetHolder`（302）/ `CompensatingBatch`（304 saga 补偿）/
  `ToolHealth`（305 工具健康）
- 成本：`CostAttributionLedger` + `CostAttributionJsonl`（334——Kubecost/OpenCost
  按标签归因：双维 chargeback 台账，嵌套 `Dimension`/`Attribution`）
- 安全：`SecretType` + `SecretScanner` + `SecretScanHook`（400——gitleaks：
  七型凭据签名三缝 MASK，输入/出站工具参数/工具结果占位符化）；
  `SecretHitStats`（嵌套 `Side`/`Hit`）+ `SecretHitStatsJsonl`
  （418——#400 扩散：三侧计数+快照 JSONL 追加导出）；
  `AuditMerkleTree`（含嵌套 `InclusionProof`/`ProofStep`）+ `AuditMerkleSeal`
  （404——CT log：时点封印出根、单条记录凭证明+根零全链验证，叶摘要与
  prev_hash 同基互证）；`AuditSealJsonl`（421——#404 扩散：封印逐行 JSONL
  追加外存+每行即时建树 verified 比对，STH 公示节奏工具面）

**buzhou-memory**

- `IdleCompactionHousekeeper` + `IdleCompactionProperties`（310——LSM 空闲压缩）

**buzhou-resilience**

- `OutputSchema` + `StructuredOutputAdvisor` + `StructuredOutputViolationException` +
  `StructuredOutputProperties`（402——instructor：JSON 契约执法+错误反馈自修复，
  修复直达模型终端/耗尽抛违规/流式直通诚实边界）
- `ShadowComparisonJsonl`（309——W&B 影子对照明细）
- `WeightedChatModel` + `BuzhouRoutingProperties`（339——LiteLLM Router：
  多模型平滑加权路由，199 原语装配收尾）；
  `RoutingWeightsHotReload`（340——权重热调：refresh 事件逐路 setWeight，
  320 rebind 同模式）

**buzhou-store-redis**

- `RedisLaneStateBackend`（316——Lua 原子泳道共享）/ `RedisVirtualKeyBudgetBackend`（315——Lua 原子配额共享）
- `RedisLeaderElector` + `LeaderElectionProperties`（331——Lua 原子选主：TTL 租约+单调纪元围栏，K8s leader election）

- 类型级快照：**+38**（收口再生，Windows 首次本机可用）；yml 键：随各 spec 入档

## effort #500–#549 新增公共面（E 会话 / spec 500–549 / impl-403–452，@since 1.0.0）

> E 会话 500 系逐轮入档（同口径：src/main 非 internal 包 public 类型）。

**buzhou-core**

- `StreamTextFilter`（500——回复流出站过滤 SPI：filter/flush 每轮新建单轮单用；
  `BuzhouHook.replyStreamFilter()` 默认方法挂点 + `HookChain.newReplyFilters()`
  hook 序收集，DefaultAgentSession 流式/非流式两缝接线）

**buzhou-guard**

- `PiiStreamRedactionHook`（500——Presidio 流式匿名化+流式 WAF 回看窗口：
  模型回复出站第三缝，滑动窗口跨 chunk 实体不漏、占位符不拆分、flush 排空；
  `buzhou.guard.pii.reply-redaction`/`reply-window` yml，默认关）

**buzhou-resilience**

- `IdempotencyAdvisor` + `BuzhouIdempotencyProperties`（501——Stripe
  Idempotency-Key：advisor 参数 `buzhou.idempotency-key` 同键重入重放首次
  终态响应（复用 ResponseCacheStore/isTerminal），链序 +440 在 response-cache
  外；enabled=true 才装配，键缺席透传零行为）

- `ModelCapabilities` + `ModelCapabilityRegistry` +
  `BuzhouModelCapabilityProperties` + `CapabilityGateAdvisor`（502——
  LiteLLM Router capabilities：vision/工具请求事前拦 ARGS_VALIDATION_FAILED，
  未注册模型零门；`buzhou.resilience.model-capabilities.<model>` 声明才装配）

- `RoutingScheduleAdjuster` + `BuzhouRoutingScheduleProperties`
  （嵌套 `RoutingWindow`）（503——K8s CronJob/Argo Rollouts schedule：
  时段窗自动切路由权重整表替换/出窗回落/同快照幂等，windows 非空才装配）

**buzhou-mcp**

- `McpServerBreaker`（504——Envoy per-host 聚合思想：一台 server 一个键
  复用 core ToolCircuitBreaker，宕机 server 全部工具快速失败结构化改道，
  `buzhou.mcp.server-breaker` 默认关，与 per-tool 131 正交两层）

**buzhou-core**

- `ExperimentBucketer` + `BuzhouExperimentProperties`（505——GrowthBook/
  Statsig：在线实验确定性分桶 sha256 mod100+字典序累积权重+未入组余量
  +曝光计数，`buzhou.experiments.<exp>.<variant>` 声明才装配）

- `ToolInputLimiter` + `ToolInputLimiterHolder`（506——nginx
  client_max_body_size：31 结果限幅的入站对称面，超限拒绝回喂结构化反馈
  不回显入参+glob per-tool 覆盖，默认 -1 零行为变化 opt-in）

- `PiiVault` + `BuzhouPiiVaultProperties`（507——Presidio Vault：可逆 PII
  代管库原语 vaultize/restore 稳定令牌 sha256|salt 去重+TTL+有界 fail-safe，
  `buzhou.guard.pii.vault.enabled` 默认关；hook 自动接线留扩散）

- `CostSpikeDetector`（嵌套 `SpikeEvent`）+ `BuzhouCostSpikeProperties`
  （508——Prometheus/Istio 滚动基线 z-score：当前分钟桶 vs 前 N 桶突刺
  +地板+minSamples+cooldown 防抖，ModelCostLedger 监听喂数 403 同缝，
  enabled=true 才装配）

- `LatencySloMonitor` + `BuzhouLatencySloProperties`（509——Google SRE
  321 时延维度扩散：坏事件=elapsed>threshold 喂 ErrorBudget，燃尽语义
  全继承，`buzhou.latency-slo.enabled` 默认关）

- `EncryptedSessionExport`（510——age/OCI 加密 artifact：seal/open 封缄
  容器复用 333 EnvelopeCipher，AAD 用途域绑定防跨域剪贴，原语先行宿主组合）

- SessionArchiver 完整性面（511——S3 checksum：写时 sha256 落独立
  命名空间 `__buzhou.archive-checksum__` + verify 五态/verifyAll，
  restore/purge/补偿级联清校验和；嵌套 `VerifyResult`/`VerifyState`）

- `PromptTemplate`（嵌套 `ValidationResult`）（512——Jinja2
  StrictUndefined：{{var}} 抽取/严格渲染缺失一次列全/未闭合语法错/
  预检面；与 401 注册表组合消费）

- `EvalFlakinessDetector`（嵌套 `FlakinessReport`/`FlakyItem`）（513——
  HELM/工业 A/A test：同指纹两 run 红绿翻转=抖动、单侧=漂移不进分母，
  纯函数 EvalRunDiff 同型）

- `WebhookDeliveryLatency`（嵌套 `Snapshot`）（514——416 分位族同法：
  成功投递时延滚动窗 exact 最近秩 p50/p95/p99 零样本 null，forwarder
  setDeliveryLatency setter 接线默认 null 零变化）

- `ContentModerationHook`（嵌套 `Action`）（515——OpenAI moderation
  本地词表面：违禁词 contains 双缝过滤 BLOCK/MASK，命中计数分缝有界，
  `buzhou.guard.moderation` 默认关）

- `JudgeCalibration`（嵌套 `CalibrationReport`）（516——LightEval judge
  calibration：verdict vs 金标准混淆矩阵四率，判红为正类，分母 0 null
  诚实空值，纯函数）

**buzhou-memory**

- `CompactionRatioStats`（嵌套 `Snapshot`）（517——416 分位族同法：
  压缩回收字符分位+逐出比直方图+折入 trigger 计数，挂 CompactionListener
  缝观测零干预，`MemoryModule.compactionStats()` 读数面）

- `SessionExportSanitizer`（518——Presidio anonymize × 28 导出面：消息/
  摘要/state 三内容域占位符化不可变副本，结构字段原样，与 510 组合
  先脱敏再封缄）

**buzhou-observability**

- `ToolGraphAnalyzer`（嵌套 `ToolGraphReport`/`Edge`/`ToolTotal`）（519——
  LangSmith trace analytics：TOOL span 同轮相邻有向边计数+per-tool
  成败错误率，纯函数+store 便捷重载）

- EvalRunner run 预算闸（520——AWS Budgets/pytest maxfail 早停语义：
  `setRunBudgetChars` 逐项估算累计超限早停，剩余项 error [RUN-BUDGET]
  三态显式 partial；0=关零行为变化）

- `PostmortemBundle`（521——317 ExportBundle 事故域预设组合：时间线/
  错误签名/成本双维 rollup 标准 ZIP+summary 汇总，源缺席跳过）

- 生命周期事件补齐（522——`session.opened` spawn 即派发：监听器挂载后
  先于任何轮次，payload 身份三元组；与既有 session.closed 配对闭环；
  无新类型）

- `FailureTurnSnapshots`（嵌套 `Snapshot`）（523——Sentry event payload：
  失败轮复现最小集快照（错误类/消息截断/输入预览），SessionObserver 缝
  423 同法，环形 128+JSONL 导出）

- DefaultMcpClientRegistry `ConnectRetryPolicy`（524——Resilience4j retry
  指数退避：建连失败 base×2^n 封顶 60s 重排、耗尽收口既有失败语义，
  `buzhou.mcp.connect-retry` 声明即启用）

- `EvalCaseAmplifier`（525——Ragas testset generation：种子→LLM 同语义
  改写候选（id 空未入库语义+人审教义），围栏剥离逐行容错，零可解析
  EVAL_OPERATION_INVALID 带预览）

- `WatermarkHealth`（526——181×312 桥接：per-session 低水位翻转态聚合
  为 context-watermark 机制健康面（低水位会话数≥阈值 DOWN），312 规则
  按机制名可订阅）

- `EvalDatasetCsv`（527——LangSmith/HF datasets CSV 互操作：RFC 4180
  toCsv/fromCsv 往返+表头宽松校验+Writer 导出，纯内存行表）

- `SessionCanaryRegistry`（嵌套 `LeakFrom`）（528——thinkst canarytokens
  /honeytoken：跨会话泄漏探测面，确定性令牌+他令牌扫描+LRU 256 有界，
  与 CanaryGuard 注入检测语义正交）

- `ToolTimeoutOverrides`（+嵌套 Holder）（529——31 per-tool glob 覆盖
  同法 × 单工具超时扩展：覆盖值替换全局、Deadline 恒天花板，默认空零变化）

- `ModelBudgetGate`（530——budget 族模型维度扩散：ModelCostLedger 记账面
  vs `buzhou.budget.model-budget.<model>` 预算，耗尽 beforeModel 拦截，
  map 非空才装配，未喂账恒放行）

- 装配绑定审计修复（531——409 result-schemas/406 deprecated/505
  experiments 单 Map 组件 record 构造绑定 prefix.<组件名> 子路径致根
  yml 绑空静默 no-op；统一改根绑定直读+内容非空回归断言）
- WebhookSignatures verifyWithRotation 重载×2（540——Stripe 多签名密钥：
  双密钥轮换验签 current→previous + 轮换×容差窗组合，fail-closed 不变；
  无新类型）

- webhook 载荷大小上限（533——Kafka max message size：outbox
  maxPayloadChars 超限拒入队+oversized 计数，默认 0 零变化，
  `buzhou.webhook.max-payload-chars`）

- `PromptVersionDiff`（嵌套 `VersionDiff`/`DiffLine`）（534——Git diff
  思想：注册表版本行级 LCS 最小变更集，晋级/回滚评审只看变化，纯函数）

- EvalRunner error 项重试一次（535——pytest flaky rerun：STATUS_ERROR
  重跑一次取第二次结果 detail [RETRIED] 留痕+计数，语义 fail 不重试，
  默认关）

- `SecretScanStreamHook`（536——400 秘密扫描第四缝（回复出站流）：
  滑动窗口跨 chunk 密钥不漏，复用 SecretScanner，500 SPI 第二消费者
  组合性证明，`secrets.stream-redaction` 默认关）

- 死信原因分类计数（537——`buzhou.webhook.dead-reason` tag reason
  有界（4xx|重试耗尽），治理动作分流；无新类型）

- `StoreFsckHousekeeper` + `BuzhouFsckProperties`（538——341 选主扩散：
  StoreFsck 只读对账定时化，findings WARN+计数不自动修复，
  `buzhou.fsck.enabled` 默认关）

**buzhou-spill**

- `ReadAuditTrail`（嵌套 `ReadRecord`）（539——spill 回读审计：readRange
  有界样本窗+per-uri 计数降序+完整性告警计数，只观测零干预，
  DiskSpillStore.readAudit() 读数面）

- `JudgeAgreement`（嵌套 `AgreementReport`）（541——scikit-learn
  cohen_kappa_score：双 judge 一致率 Cohen κ 修正机遇一致+Landis-Koch
  分级，纯函数 516 同型）

- `WebhookDeadLetterJsonl`（542——60/67 导出族同构：死信清单一行一
  JSON 转义完备单行+行数返回，源经 forwarder.deadLetters() 查询）

- `SpanStatusDistribution`（543——Prometheus label 聚合：kind×status
  计数读数（大小写归一+UNSET 兜底），纯函数+store 便捷重载）

- `EvalRunDurationStats`（嵌套 `DurationStats`/`SlowestItem`）（544——
  416 分位族同法：run 项耗时 p50/p95/max+最慢 top3，纯函数读数）

- `PromptRegistrySnapshot`（545——Langfuse export/import：注册表全量
  快照可移植 JSON（版本史+labels），导入空注册表按旧版本序重放，
  非空 fail-fast）

**buzhou-skills**

- `SkillBodyAudit`（嵌套 `Report`/`SkillRow`）（546——110 目录预算
  per-skill 深化：正文字符规模降序+预算超限标记+聚合统计，纯函数读数）

- `SessionExportChecksum`（547——S3 checksum 明文通道对偶：导出 JSON
  sha256 校验和+verify fail-closed，防衰变/误写不防蓄意同改（归 510））

- `StoreFsckHealth`（548——538 巡检健康面接入：mechanism=store-fsck
  观测面恒 UP，details 聚合 runs/totalFindings/lastFindings/
  skippedNotLeader）

- GuardModule `assemblySummary()`（549——装配 hook 名列表读数，支持包
  /排障「guard 挂了哪些钩子」一屏可读；加法方法无新类型）

## effort #700–#749 新增公共面（G 会话 / spec 700–749 / impl-600–649，@since 1.0.0）

> G 会话 700 系逐轮入档（同口径：src/main 非 internal 包 public 类型）。

**buzhou-resilience**

- `CapabilityDecisionAudit`（嵌套 `Decision`/`Report`）（700——OPA Decision
  Logs 思想：能力门 deny 环形留痕容量 64+dropped 计数、admit 只计数、
  denyByModel 聚合、snapshot() 不可变报告；CapabilityGateAdvisor 3 参
  构造接线，纯旁路拒绝行为零变化）

- SemanticCacheStore 权重预算族（701——`maxWeightChars` 5 参构造+
  `maxWeightChars()`/`totalWeightChars()`/`weightEvictionCount()` 读数+
  estimateChars 估算口径；evictedCount 口径不混；加法方法无新顶层类型）

- `CircuitTransitionJournal`（嵌套 `Transition`/`Report`）（702——
  Resilience4j EventConsumer 思想：进程级变迁环形留痕容量 64+dropped、
  per-model trips/recoveries/halfOpens 聚合；内嵌 ModelCircuitBreaker
  恒开旁路，`transitionJournal()` getter 暴露）

- `RoutingHealthDampener`（703——HAProxy agent-check 思想：attach 原语
  跳闸压权至地板/恢复回声明权重；ModelCircuitBreaker 加
  `addTransitionListener` 监听缝，listener 异常隔离不伤状态机）

- `PromptComposition`（嵌套 `Section`/`Report`）（704——Langfuse prompt
  analytics 思想：analyze(Prompt) 按角色聚合 chars/messages/share 降序+
  字典序稳定；TOOL 载荷从 getResponses()responseData 计量；纯函数读数）

- `RedisKeyLayoutAudit`（嵌套 `Finding`）（705——fsck 思想：audit(prefix)
  结构性对抗模拟产出三族碰撞 Finding（RESERVED_SEGMENT/
  SPAN_INDEX_CLASH/COLON_SUFFIX_TRICK）+reservedSegments() 读数+
  isSafeSessionId 摄入守卫谓词；纯静态不改键形状）

- `McpDirectoryDiff`（嵌套 `SyncStatus`/`ChangeKind`/`ToolChange`/
  `ServerDiff`/`Report`）（706——ArgoCD diff 思想：两份 toolHints 快照
  plan 式差异，per-server 四态+三类变更+危险方向翻转 risky 标记；
  纯函数无状态字典序确定序）

- `SpillPairAudit`（嵌套 `Finding`/`Report`）（707——Git fsck 思想：
  audit(rootDir) 只读扫 .spill/.meta 配对残缺（DATA_WITHOUT_META 带字节/
  META_WITHOUT_DATA），双写崩溃窗口的配额吞噬证据；三层完整性矩阵中层）

- EvalRunner `setMemoizationKey`（708——scikit-learn Pipeline memory 思想：
  项级结果记忆化 opt-in，sig=sha256(dataset|itemId|input|expected|key)，
  命中 detail `[MEMO]` 前缀+hits/misses 计数，ERROR 不缓存；加法方法
  无新顶层类型）

- ExperimentBucketer 到期族（709——GrowthBook feature expiry：构造器扩
  `expiresAt`+`Clock`，assign() 过期按未入组返回 null+`__expired__` 独立
  桶+每实验一次 WARN，`expiredExperiments()`/`expiresAt(name)` 读数；
  加法方法无新顶层类型）

- ExperimentBucketer holdout 层（710——Statsig holdout layer：构造器再扩
  `holdoutPercent`，sha256("holdout|unitKey") 跨实验一致排除+`__holdout__`
  独立桶，`holdoutPercent()` 读数；加法方法无新顶层类型）

- `TurnSequenceAudit`（嵌套 `Marker`/`Finding`）（711——Kafka offset 审计
  思想：audit(List<Marker>) 单遍判消息序列 GAP/DUPLICATE/OUT_OF_ORDER，
  调用方投影解耦 store SPI；纯函数只读）

- SpanStatusDistribution `healthSummary`（嵌套 `HealthSummary`）（720
  修正轮——R13 曾在 core 重建撞 543；增量收敛到既有 analytics 类：
  runningResidue 泄漏信号+errorRate 口径显式；无新顶层类型）

- EvalDatasetMeta `tags` 组件 + EvalDatasetStore `tagDataset`/`untagDataset`/
  `listDatasetsByTag`（713——Langfuse dataset tags 思想：归一 `[a-z0-9:-]`
  幂等打标/圈选；旧记录解码空表零迁移；tags 不入 fingerprint）

- BuiltInEvaluators `similarity(minRatio)`（714——HELM grading scales 思想：
  字符 trigram Jaccard 模糊判定，detail 携带分数留痕；minRatio∈[0,1]
  fail-fast；加法方法无新顶层类型）

- `FormatPreservingMasker`（715——Presidio format-preserving 思想的
  结构化简化版：maskPhone/maskIdCard/maskEmail/maskIp+通用 mask 保长
  打星，形状校验复用 PiiDetector 口径、失败全星 fail-closed；纯静态）

- `TodoStalenessAudit`（嵌套 `Row`/`Report`）（716——agent todo 纪律面板：
  analyze 轮次年龄+滞留清单降序+promptHint 一行人话；staleAfterTurns≤0
  只报统计；纯读数不自动清理）

- `FactConflictAudit`（嵌套 `Kind`/`Row`/`Report`）（717——mem0 冲突治理
  思想：audit(List<SharedFact>) 按键分组判 CONFLICT/DUPLICATE，entries
  证据全列；Objects.equals 保守口径；纯函数快照审计）

- EvalRunner `setDriftBaseline`/`lastDriftDelta`（718——Evidently drift
  思想：opt-in 通过率基线漂移告警，基线=同数据集早于本次最近 window 次
  均值（防自污染），|Δ|≥warnShift WARN+计数；复用 run 落盘零新存储；
  加法方法无新顶层类型）

- `ProviderRateLimitSignals`（嵌套 `Pressure`/`Signals`）（719——OpenAI
  x-ratelimit 头思想：parse(HttpHeaders) 解析余量/上限/reset+utilization
  +三级压力分级；全 null-safe fail-safe 无头 empty；纯静态解析原语）

- `ChunkingEmbeddingModel`（721——OpenAI embeddings 批量上限思想：
  call 按 maxBatchSize 切块顺序调 delegate+全局 index 重排拼接，≤max
  直通；default embed 方法经 call 自动受益）

- SemanticCache `embeddingMaxBatch` 组件（723——721 装配兑现：yml
  semantic-cache.embedding-max-batch（默认 0=关），ResilienceModule 在
  语义缓存装配点包装 ChunkingEmbeddingModel；metadata 登记；加法组件
  +兼容构造，无新顶层类型）

- `StateTtlCoverage`（嵌套 `Row`/`Report`）（724——S3 生命周期审计思想：
  analyze(Map<String,StateEntry>) 永生键计数+coverage（total=0 空真
  1.0）+byProducer 归因；纯函数读数）

- RoutingHealthDampener 半开中点档（725——HAProxy slow-start：HALF_OPEN
  权重=(floor+declared)/2 向下取整，CLOSED 回声明值；加法行为无新类型）

- `EventTypeDistribution`（嵌套 `Row`/`Report`）（726——Grafana Loki top-k
  思想：of(List<EventRecord>) type 计数降序+字典序稳定+topType 占比；
  类型不假设闭集；纯函数读数）

- `RedisKeyLayoutHealth`（727——705 审计接线：implements BuzhouHealth，
  mechanism=redis-key-layout 恒 UP，details 聚合三族碰撞计数+保留段；
  随 BuzhouRedisStoreAutoConfiguration 条件装配）

- `SpillPairHealth`（728——707 审计接线：implements BuzhouHealth，
  mechanism=spill-pair 禁用 UNKNOWN/启用恒 UP，details 五项统计；
  随 BuzhouSpillHealthAutoConfiguration 装配）

- `ObservabilityCapacityHealth`（729——容量逐出读数接线：implements
  BuzhouHealth，mechanism=memory-observability 恒 UP，details
  used/max/utilization/evicted；InMemoryObservabilityStore 加逐出计数
  与三读数（sessionCount 提升 public））

- ProviderRateLimitSignals `parseFlexible`（730——719 扩散：跨供应商归一，
  OpenAI 头优先不混合来源，缺项回退 Anthropic anthropic-ratelimit-*；
  加法方法无新顶层类型）

- `EvalScoreAnalytics`（嵌套 `Report`）（731——714 消费端：similarityScores
  正则解析 run 明细中的 similarity= 分数→scored/min/max/mean/scores；
  无分数项跳过诚实计数；纯函数）

- `EventPayloadSizeAudit`（嵌套 `Row`/`Report`）（732——Sentry payload 限额
  思想：analyze(List<EventRecord>) Jackson 序列化字节按类型聚合
  （count/total/max，totalBytes 降序）+serialized/skipped 诚实计数；
  纯函数读数）

- ResponseCacheStore 权重预算族（737——701 对称落地：maxWeightChars
  4 参构造+maxWeightChars/totalWeightChars/weightEvictionCount 读数+
  替换同键回收/TTL 即弃回收；加法方法无新顶层类型）

- `PromptUsageGaps`（嵌套 `Report`）（733——401×使用统计联合读数：
  analyze(declaredNames, rows) 零使用差集+孤儿统计漂移信号；纯函数）

- `EventPairingAudit`（嵌套 `Finding`/`Report`）（735——请求/应答型事件
  spanId 内 min 配对，差集产出 UNPAIRED_REQUEST/UNPAIRED_RESPONSE+paired
  计数；规则表调用方供给；纯函数）

- `SpanParentIntegrityAudit`（嵌套 `Finding`/`Report`）（736——OTel trace
  树语义：audit(List<SpanRecord>) 悬空父引用发现+totalSpans/rootSpans；
  纯函数）

- EvalRunner `lastFingerprintChanged`（734——82 指纹消费信号：当前 vs 最近
  历史 run 指纹不同置位+计数+INFO；diff 明细归 EvalRunDiff；加法方法
  无新顶层类型）

- `McpConcurrencyView`（722——610 并发闸读数面：server/limit/available/
  inFlight 快照，limit=-1 哨兵=未设；McpClientRegistry 加 default
  `concurrencyViews()`，DefaultMcpClientRegistry 覆写）

