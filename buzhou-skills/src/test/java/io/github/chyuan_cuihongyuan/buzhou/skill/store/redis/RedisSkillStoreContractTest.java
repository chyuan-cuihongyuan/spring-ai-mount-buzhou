package io.github.chyuan_cuihongyuan.buzhou.skill.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.skill.SkillStatus;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.AbstractSkillStoreContractTest;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.DbSkillRecord;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RedisSkillStore} 契约接入（K 会话 R3 / spec 1202 / T1809，Testcontainers
 * redis:7-alpine，无 Docker 跳过——store-redis 同口径）+ 持久语义加验（新 store
 * 实例同 Redis = 重启后行仍在，JSON round-trip 经真实网络）。
 * 先例：RedisSessionIndexContractTest（门控 / 清场 / 持久语义）。
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisSkillStoreContractTest extends AbstractSkillStoreContractTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private static LettuceConnectionFactory factory;
    private static RedisSkillStore storeRef;

    @Override
    protected SkillStore store() {
        if (storeRef == null) {
            factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
            factory.afterPropertiesSet();
            storeRef = new RedisSkillStore(new StringRedisTemplate(factory));
        }
        return storeRef;
    }

    @AfterEach
    void cleanUp() {
        if (storeRef != null) {
            // 公共 API 清场（级联清资源 hash），不复制私有键前缀
            storeRef.findAll().forEach(record -> storeRef.deleteByName(record.name()));
        }
    }

    @AfterAll
    static void closeFactory() {
        if (factory != null) {
            factory.destroy();
        }
    }

    /** 持久语义：新 store 实例（模拟重启）同 Redis 可见全部行。 */
    @Test
    void rowsSurviveNewStoreInstanceOverSameRedis() {
        store().save(record("persist-skill", null, SkillStatus.PUBLISHED));

        LettuceConnectionFactory restartedFactory =
                new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        restartedFactory.afterPropertiesSet();
        try {
            RedisSkillStore restarted =
                    new RedisSkillStore(new StringRedisTemplate(restartedFactory));

            assertThat(restarted.findByName("persist-skill")).isPresent();
            assertThat(restarted.findPublished("persist-skill")).isPresent();
        } finally {
            restartedFactory.destroy();
        }
    }
}
