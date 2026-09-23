package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Arrays;
import java.util.List;

/**
 * 语义化版本序（spec 4033 / T6067 / impl 2134）——Semantic
 * Versioning 2.0.0 优先级思想（semver.org §11）：三段数值序 +
 * pre-release 低于正式版 + 标识符逐段比（纯数字段数值比、
 * 含字母段 ASCII 字典序、数字段 &lt; 字母段、前缀全等字段多者
 * 高）+ build 元数据不参与优先级——字符串字典序排版本的病解
 * （{@code 1.0.10} 字典序 &lt; {@code 1.0.9}、{@code -rc} 被排到
 * 正式版之后的机器性错误）。
 *
 * <p>{@code parse} 全量语法校验 fail-fast（数字段禁前导零等
 * §2 规则）；嵌套 {@link Version} record 不另立面。
 */
public final class SemVerOrder {

    private SemVerOrder() {
    }

    /**
     * 解析并校验版本串（§2 语法；非法 fail-fast）。
     *
     * @param version 形如 {@code major.minor.patch[-pre][+build]}
     * @return 结构化版本
     * @throws IllegalArgumentException 语法非法（缺段/前导零/空段/非法字符）
     */
    public static Version parse(String version) {
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("版本串非空");
        }
        int buildStart = version.indexOf('+');
        int preStart = version.indexOf('-');
        String core = version;
        String prerelease = null;
        String build = null;
        if (buildStart >= 0) {
            build = version.substring(buildStart + 1);
            core = version.substring(0, buildStart);
        }
        if (preStart >= 0 && (buildStart < 0 || preStart < buildStart)) {
            prerelease = core.substring(preStart + 1);
            core = core.substring(0, preStart);
        }
        String[] parts = core.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("需三段数字：" + version);
        }
        int major = numericPart(parts[0], version);
        int minor = numericPart(parts[1], version);
        int patch = numericPart(parts[2], version);
        List<String> preIds = prerelease == null ? List.of() : identifierPart(prerelease, version, true);
        if (build != null) {
            identifierPart(build, version, false);
        }
        return new Version(major, minor, patch, preIds, build == null ? "" : build);
    }

    /** 优先级比较（§11；build 元数据不参与——同优先级即相等）。 */
    public static int compare(Version left, Version right) {
        int c = Integer.compare(left.major(), right.major());
        if (c != 0) {
            return c;
        }
        c = Integer.compare(left.minor(), right.minor());
        if (c != 0) {
            return c;
        }
        c = Integer.compare(left.patch(), right.patch());
        if (c != 0) {
            return c;
        }
        return comparePrerelease(left.prerelease(), right.prerelease());
    }

    private static int comparePrerelease(List<String> left, List<String> right) {
        if (left.isEmpty() && right.isEmpty()) {
            return 0;
        }
        if (left.isEmpty()) {
            return 1;   // 无 pre-release 高于有
        }
        if (right.isEmpty()) {
            return -1;
        }
        int shared = Math.min(left.size(), right.size());
        for (int i = 0; i < shared; i++) {
            int c = compareIdentifier(left.get(i), right.get(i));
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(left.size(), right.size());   // 前缀全等——字段多者高
    }

    private static int compareIdentifier(String left, String right) {
        boolean leftNumeric = isNumeric(left);
        boolean rightNumeric = isNumeric(right);
        if (leftNumeric && rightNumeric) {
            return compareNumeric(left, right);
        }
        if (leftNumeric) {
            return -1;   // 数字段 < 字母段
        }
        if (rightNumeric) {
            return 1;
        }
        return left.compareTo(right);
    }

    /** 无溢出数值比较（禁前导零 ⇒ 先比长度再字典序）。 */
    private static int compareNumeric(String left, String right) {
        int c = Integer.compare(left.length(), right.length());
        return c != 0 ? c : left.compareTo(right);
    }

    private static boolean isNumeric(String identifier) {
        return identifier.chars().allMatch(ch -> ch >= '0' && ch <= '9');
    }

    private static int numericPart(String part, String whole) {
        if (part.isEmpty() || !isNumeric(part)) {
            throw new IllegalArgumentException("数字段非法：" + whole);
        }
        if (part.length() > 1 && part.charAt(0) == '0') {
            throw new IllegalArgumentException("数字段禁前导零：" + whole);
        }
        return Integer.parseInt(part);
    }

    /** pre-release/build 标识符段拆分校验（pre 段禁纯数字前导零；build 段只查字符）。 */
    private static List<String> identifierPart(String raw, String whole, boolean isPrerelease) {
        String[] ids = raw.split("\\.", -1);
        for (String id : ids) {
            if (id.isEmpty()) {
                throw new IllegalArgumentException("空标识符段：" + whole);
            }
            for (int i = 0; i < id.length(); i++) {
                char ch = id.charAt(i);
                boolean legal = (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'z')
                        || (ch >= 'A' && ch <= 'Z') || ch == '-';
                if (!legal) {
                    throw new IllegalArgumentException("标识符含非法字符 '" + ch + "'：" + whole);
                }
            }
            if (isPrerelease && isNumeric(id) && id.length() > 1 && id.charAt(0) == '0') {
                throw new IllegalArgumentException("pre-release 数字段禁前导零：" + whole);
            }
        }
        return Arrays.asList(ids);
    }

    /**
     * 结构化版本（不可变值对象）。
     *
     * @param major 主版本
     * @param minor 次版本
     * @param patch 修订号
     * @param prerelease pre-release 标识符段（无则空列表）
     * @param build build 元数据原文（无则空串；不参与优先级）
     */
    public record Version(int major, int minor, int patch, List<String> prerelease, String build) {

        /** 严格高于（优先级严格更大——build 差异不算更高）。 */
        public boolean newerThan(Version other) {
            return compare(this, other) > 0;
        }

        /** 原文重建（build 段保留）。 */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder()
                    .append(major).append('.').append(minor).append('.').append(patch);
            if (!prerelease.isEmpty()) {
                sb.append('-').append(String.join(".", prerelease));
            }
            if (!build.isEmpty()) {
                sb.append('+').append(build);
            }
            return sb.toString();
        }
    }
}
