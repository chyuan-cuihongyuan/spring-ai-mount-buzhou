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
- `public record WebhookDeadLetter`（webhook）；`WebhookOutbox` 为包私有（非公开面）
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
