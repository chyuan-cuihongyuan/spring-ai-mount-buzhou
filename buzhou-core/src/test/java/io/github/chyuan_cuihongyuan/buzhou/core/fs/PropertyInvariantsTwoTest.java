package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import io.github.chyuan_cuihongyuan.buzhou.core.budget.VirtualKeys;
import org.junit.jupiter.api.RepeatedTest;

import java.util.SplittableRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 208 §B / T572：性质测试 II（spec 180 fog「租户正则/虚拟键守恒」收口）：
 * ①任意含非法字符的租户 id 必被拒（随机构造违规串）；②合法 id 必落在
 * tenants/<t> 下；③VirtualKeys 守恒——注册限额 = 可用余额上限（耗尽后
 * trySpend 恒 false 不凭空恢复）。种子固定可复现。
 */
class PropertyInvariantsTwoTest {

    private static final SplittableRandom RANDOM = new SplittableRandom(7);

    /** 不变量①：随机违规租户 id 必拒。 */
    @RepeatedTest(100)
    void invalidTenantIdsAlwaysRejected() {
        String bad = randomBadTenantId();
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> FileSandbox.forTenant(java.nio.file.Path.of("."), bad))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 不变量②：随机合法 id 的沙箱根必在 tenants/<t> 下且表为空（零追加白名单）。 */
    @RepeatedTest(100)
    void validTenantRootsIsolated() {
        String good = randomGoodTenantId();
        FileSandbox sandbox = FileSandbox.forTenant(java.nio.file.Path.of("."), good);
        assertThat(sandbox.root().toString()).endsWith("tenants/" + good);
        assertThat(sandbox.resolve("a/b.txt").startsWith(sandbox.root())).isTrue();
    }

    /** 不变量③：虚拟键守恒——耗尽态下任意正额尝试恒 false，直到 reset。 */
    @RepeatedTest(50)
    void virtualKeyExhaustionIsSticky() {
        VirtualKeys keys = VirtualKeys.create();
        long limit = RANDOM.nextLong(1, 10_000);
        keys.register("k", limit);
        assertThat(keys.trySpend("k", limit)).isTrue(); // 恰好用满
        for (int i = 0; i < 5; i++) {
            assertThat(keys.trySpend("k", RANDOM.nextLong(1, 100))).isFalse();
            assertThat(keys.isExhausted("k")).isTrue();
        }
        keys.reset("k");
        assertThat(keys.isExhausted("k")).isFalse(); // reset 后恢复——不凭空
    }

    /** 随机拼一个必违规的租户 id（注入非法字符/超长/大写之一）。 */
    private static String randomBadTenantId() {
        String base = randomGoodTenantId();
        return switch (RANDOM.nextInt(4)) {
            case 0 -> base + (char) ('A' + RANDOM.nextInt(26)); // 大写
            case 1 -> base + "_"; // 尾连字符
            case 2 -> base + "../" + randomGoodTenantId(); // 穿越
            default -> "x".repeat(33 + RANDOM.nextInt(10)); // 超长
        };
    }

    private static String randomGoodTenantId() {
        int len = RANDOM.nextInt(1, 33);
        StringBuilder sb = new StringBuilder();
        sb.append((char) ('a' + RANDOM.nextInt(26))); // 首字符字母数字
        for (int i = 1; i < len; i++) {
            sb.append(RANDOM.nextBoolean() && i < 12
                    ? (char) ('0' + RANDOM.nextInt(10))
                    : (char) ('a' + RANDOM.nextInt(26)));
        }
        return sb.toString();
    }
}
