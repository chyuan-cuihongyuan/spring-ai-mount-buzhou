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
- `ApiSurfaceSnapshotTest` + `docs/api-surface.snapshot.txt`（449 类型黄金快照）
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
