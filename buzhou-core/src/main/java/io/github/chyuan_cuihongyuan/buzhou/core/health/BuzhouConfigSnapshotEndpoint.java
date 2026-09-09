package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.util.Map;
import java.util.TreeMap;

/**
 * 生效配置自描述端点 {@code /actuator/buzhou-config}（spec 343 / T677，
 * Spring Boot actuator configprops + sanitization 借鉴）：枚举 Environment
 * 全部 {@code buzhou.*} <b>生效</b>属性（含 env/命令行覆盖——真实生效值
 * 而非声明值）；密钥类键掩码 {@code ***}（宽匹配宁掩勿漏——333 master-key
 * 等绝不出端点）；无属性空 map 诚实；只读、键有序稳定。
 */
@Endpoint(id = "buzhou-config")
public final class BuzhouConfigSnapshotEndpoint {

    /** 掩码标记。 */
    public static final String MASK = "***";

    private static final String[] SENSITIVE_SUBSTRINGS =
            {"key", "secret", "password", "token", "credential"};

    private final Environment environment;

    public BuzhouConfigSnapshotEndpoint(Environment environment) {
        this.environment = java.util.Objects.requireNonNull(environment);
    }

    @ReadOperation
    public Map<String, String> buzhouConfig() {
        Map<String, String> snapshot = new TreeMap<>();
        if (!(environment instanceof ConfigurableEnvironment configurable)) {
            return snapshot;
        }
        for (PropertySource<?> source : configurable.getPropertySources()) {
            if (!(source instanceof EnumerablePropertySource<?> enumerable)) {
                continue;
            }
            for (String name : enumerable.getPropertyNames()) {
                if (!name.startsWith("buzhou.")) {
                    continue;
                }
                Object value = environment.getProperty(name);
                if (value != null) {
                    snapshot.put(name, maskIfNeeded(name, String.valueOf(value)));
                }
            }
        }
        return snapshot;
    }

    /**
     * 宽匹配掩码（<b>末段属性名</b>小写含敏感子串即掩——宁掩勿漏；整键匹配
     * 会误伤 buzhou.token-budget.* 等含 token 的命名空间，掩码判定只看
     * 最后一段的属性名）。
     */
    public static String maskIfNeeded(String name, String value) {
        String lastSegment = name.substring(name.lastIndexOf('.') + 1)
                .toLowerCase(java.util.Locale.ROOT);
        for (String sensitive : SENSITIVE_SUBSTRINGS) {
            if (lastSegment.contains(sensitive)) {
                return MASK;
            }
        }
        return value;
    }
}
