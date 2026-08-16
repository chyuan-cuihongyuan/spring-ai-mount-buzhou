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
