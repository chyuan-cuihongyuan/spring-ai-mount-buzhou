package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.ConfigMaps;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditChain;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditRecordStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditTrailCollector;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.InMemoryAuditRecordStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.JdbcAuditRecordStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.PemFileKeyPersister;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.PemFileKeyProvider;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyPersister;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyProvider;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyRing;
import io.github.chyuan_cuihongyuan.buzhou.guard.hook.GuardAuthApi;
import io.github.chyuan_cuihongyuan.buzhou.guard.policy.PolicyRefresher;
import io.github.chyuan_cuihongyuan.buzhou.guard.policy.ResourcePolicySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.List;

/**
 * HITL 危险守卫自装配（spec 07 / 09 / ticket 22）。
 *
 * <p>装配 {@link GuardModule}，把 {@link GuardModule#configure()} 产出注册为 {@link RuntimeConfig} bean；
 * 另暴露 {@link GuardAuthApi}（业务侧 REST 授权写回用）。dangerous-tools 清单经
 * {@code buzhou.guard.dangerous-tools} 配置驱动（config-driven，保持模块解耦、不自动耦合 tools）。
 *
 * <p>impl-39 / spec 13 §T64：审计链默认在线——{@code buzhou.guard.audit.*} 驱动
 * AuditRecordStore（auto=有 DataSource 即 JDBC append-only，否则 InMemory 有界环形）、
 * SigningKeyRing（PEM 文件密钥版本化；缺失降级纯哈希链 + WARN 不阻断）、
 * AuditChain（从持久化记录续链，跨重启不断）与 AuditTrailCollector（会话监听入口）。
 *
 * <p>事实采集器（{@code FactCollectorHook}）属程序化进阶能力，本装配不自动接线；需要时由业务侧
 * 经 {@link GuardModule.Builder#factDefinition} 构建，FactAttachmentRenderer 经 AttachmentRenderer
 * SPI 由 memory 组合（同 todo 渲染器路径）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.guard", name = "enabled", matchIfMissing = true)
public class BuzhouGuardAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(BuzhouGuardAutoConfiguration.class);

    @Bean
    public GuardModule guardModule(BuzhouStores stores, Environment env,
            ObjectProvider<PolicyRefresher> policyRefresher) {
        PolicyRefresher refresher = policyRefresher.getIfAvailable();
        GuardModule.Builder builder = GuardModule.builder(stores)
                .fromYml(ConfigMaps.sub(env, "buzhou.guard"));
        if (refresher != null) {
            builder.policyEngine(refresher);
        }
        return builder.build();
    }

    @Bean
    public GuardAuthApi guardAuthApi(GuardModule module) {
        return module.authApi();
    }

    @Bean
    public RuntimeConfig guardRuntimeConfig(GuardModule module) {
        return module.configure();
    }

    /**
     * spec 17 / impl-60（T85 沙箱合流）：应用注册了 {@code CommandSandbox} bean（Deno/E2B/
     * Firecracker/Limited 组合，档位选择归应用）即桥接为 core 的 {@code CommandBackend}，
     * 供 tools 的 run_command 沙箱委托版消费（buzhou.tools.command.backend=sandbox）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(
            type = "io.github.chyuan_cuihongyuan.buzhou.guard.sandbox.CommandSandbox")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(
            type = "io.github.chyuan_cuihongyuan.buzhou.core.exec.CommandBackend")
    public io.github.chyuan_cuihongyuan.buzhou.core.exec.CommandBackend sandboxCommandBackend(
            io.github.chyuan_cuihongyuan.buzhou.guard.sandbox.CommandSandbox sandbox) {
        return new io.github.chyuan_cuihongyuan.buzhou.guard.sandbox.SandboxCommandBackend(sandbox);
    }

    @Bean
    @ConditionalOnProperty(prefix = "buzhou.guard.audit", name = "enabled", matchIfMissing = true)
    public AuditRecordStore auditRecordStore(Environment env,
            ObjectProvider<DataSource> dataSource) {
        GuardAuditConfig config = auditConfig(env);
        boolean jdbcAvailable = dataSource.getIfAvailable() != null;
        if (GuardAuditConfig.STORE_JDBC.equals(config.store())
                || (GuardAuditConfig.STORE_AUTO.equals(config.store()) && jdbcAvailable)) {
            if (!jdbcAvailable) {
                LOG.warn("buzhou.guard.audit.store=jdbc 但无 DataSource bean——回退 InMemory 有界环形");
                return new InMemoryAuditRecordStore(config.inMemoryCapacity());
            }
            return new JdbcAuditRecordStore(
                    new org.springframework.jdbc.core.JdbcTemplate(dataSource.getObject()));
        }
        return new InMemoryAuditRecordStore(config.inMemoryCapacity());
    }

    @Bean
    @ConditionalOnProperty(prefix = "buzhou.guard.audit", name = "enabled", matchIfMissing = true)
    public SigningKeyRing signingKeyRing(Environment env) {
        GuardAuditConfig config = auditConfig(env);
        List<SigningKeyProvider.VersionedSigningKey> keys = new java.util.ArrayList<>();
        if (!config.keyFiles().isEmpty()) {
            keys.addAll(new PemFileKeyProvider(config.keyFiles().stream()
                    .map(keyFile -> new PemFileKeyProvider.Entry(keyFile.version(),
                            keyFile.privateKeyPath(), keyFile.publicKeyPath()))
                    .toList()).load());
        }
        // spec 41 §A / T153：key-dir 目录扫描（v<version>.pem 约定命名）——运行期轮换写入的
        // 新钥重启后自动入环；非空时同时给环挂轮换持久化器（写而后切）
        SigningKeyPersister persister = null;
        if (config.keyDir() != null) {
            keys.addAll(PemFileKeyProvider.scanDirectory(config.keyDir()).load());
            persister = new PemFileKeyPersister(config.keyDir());
        }
        SigningKeyRing ring = new SigningKeyRing(config.minVerifyVersion(), keys, persister);
        if (!ring.hasSigningKey()) {
            LOG.warn("buzhou.guard.audit 无签名密钥（signing.keys 未配置/为空）——"
                    + "审计链降级为纯哈希链（完整性仍可验，不可否认性弱）；"
                    + "配置 PKCS#8 PEM 路径即可启用签名");
        } else {
            LOG.info("buzhou-guard 审计签名就绪（activeVersion={}，可验版本={}，minVerifyVersion={}，"
                            + "轮换持久化={}）",
                    ring.activeVersion(), ring.registeredVersions(), ring.minVerifyVersion(),
                    persister != null ? "key-dir=" + config.keyDir() : "off");
        }
        return ring;
    }

    @Bean
    @ConditionalOnProperty(prefix = "buzhou.guard.audit", name = "enabled", matchIfMissing = true)
    public AuditChain auditChain(Environment env, SigningKeyRing keyRing,
            AuditRecordStore store) {
        AuditChain chain = new AuditChain("buzhou-guard", null, keyRing);
        List<io.github.chyuan_cuihongyuan.buzhou.guard.audit.AgentAuditRecord> persisted =
                store.loadAll();
        if (!persisted.isEmpty()) {
            chain.resume(persisted);
            LOG.info("审计链从持久化续接（既有记录 {} 条）", persisted.size());
        }
        return chain;
    }

    @Bean
    @ConditionalOnProperty(prefix = "buzhou.guard.audit", name = "enabled", matchIfMissing = true)
    public AuditTrailCollector auditTrailCollector(AuditChain chain, AuditRecordStore store) {
        return new AuditTrailCollector(chain, store);
    }

    /**
     * impl-40 / spec 13 §T64：策略门热加载（默认关——deny-by-default 引擎须显式启用）；
     * source 支持 classpath:/file:/裸路径，refresh-interval<=0 关闭轮询；启动首载失败
     * fail-fast（启用而来源不可用必须显式暴露，不静默全拒）。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "buzhou.guard.policy", name = "enabled",
            havingValue = "true")
    public PolicyRefresher policyRefresher(Environment env) {
        GuardPolicyConfig config = GuardPolicyConfig.fromGuardMap(
                ConfigMaps.sub(env, "buzhou.guard"));
        return new PolicyRefresher(new ResourcePolicySource(config.sourceLocation()),
                config.refreshInterval());
    }

    /**
     * impl-30 / spec 13 §core-1：guard 停机 lifecycle（phase
     * {@link io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases#GUARD}）；
     * impl-39：审计链停机终局自检（断链 WARN）；impl-40：策略轮询随停机关闭。
     */
    @Bean
    public GuardModuleLifecycle guardModuleLifecycle(ObjectProvider<AuditChain> auditChain,
            ObjectProvider<SigningKeyRing> keyRing, ObjectProvider<PolicyRefresher> refresher) {
        return new GuardModuleLifecycle(auditChain.getIfAvailable(),
                keyRing.getIfAvailable(), refresher.getIfAvailable());
    }

    private GuardAuditConfig auditConfig(Environment env) {
        return GuardAuditConfig.fromGuardMap(ConfigMaps.sub(env, "buzhou.guard"));
    }
}
