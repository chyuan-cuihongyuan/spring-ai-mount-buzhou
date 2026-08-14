package io.github.chyuan_cuihongyuan.buzhou.guard.policy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * classpath / file 策略来源（impl-40 / spec 13 §T64）：{@code classpath:buzhou-policy.json}
 * 与 {@code file:/etc/buzhou/policy.json} 两种寻址统一封装；etag = 内容 sha256。
 */
public final class ResourcePolicySource implements PolicySource {

    private final String location;
    private final ClassLoader classLoader;

    public ResourcePolicySource(String location) {
        this(location, ResourcePolicySource.class.getClassLoader());
    }

    public ResourcePolicySource(String location, ClassLoader classLoader) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("策略来源地址为空");
        }
        this.location = location.trim();
        this.classLoader = classLoader;
    }

    /** {@code classpath:xxx} / {@code file:xxx} / 裸路径（按 file 处理）。 */
    public static ResourcePolicySource of(String location) {
        return new ResourcePolicySource(location);
    }

    @Override
    public Snapshot load(String ifNoneMatch) {
        String content = readContent();
        String etag = sha256(content);
        if (etag.equals(ifNoneMatch)) {
            return null; // 304 语义：内容未变化
        }
        return new Snapshot(etag, PolicyRuleParser.parse(content));
    }

    private String readContent() {
        try {
            if (location.startsWith("classpath:")) {
                String resource = location.substring("classpath:".length());
                try (InputStream in = classLoader.getResourceAsStream(resource)) {
                    if (in == null) {
                        throw new IllegalStateException("策略来源 classpath 资源不存在：" + resource);
                    }
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            Path path = Path.of(location.startsWith("file:") ? location.substring(5) : location);
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("策略来源不可读：" + description(), e);
        }
    }

    @Override
    public String description() {
        return location;
    }

    private static String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
