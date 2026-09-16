package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.Locale;

/**
 * 版本要求判定（spec 2039 / T3181 / impl 1590）——npm semver range 思想：
 * 技能声明运行时要求（requires: ^1.2.0 / ~2.1 / >=0.9 / 1.4.2 精确 /
 * * 任意），本件解析并判定实际版本满足否——版本比较语义与
 * SkillChannelResolver 同口径（点分逐段、prerelease 低于同基段）。
 *
 * <p>CARET ^x.y.z：同主版本内兼容（&gt;=x.y.z &lt;(x+1).0.0）；0.x 主
 * 次锁定（^0.2.3 → &lt;0.3.0）、0.0.x 补丁锁定（semver 0.x 惯例）。
 * TILDE ~x.y.z：次版本锁定（&lt;x.(y+1).0）。不可变、线程安全。
 */
public final class VersionRequirement {

    /** 范围算子六型。 */
    public enum Operator {
        CARET,   // ^ 同主兼容（0.x 特例锁定）
        TILDE,   // ~ 次版本锁定
        GTE,     // >= 下界含
        GT,      // > 下界不含
        EXACT,   // 无算子——精确匹配
        ANY      // * 任意
    }

    private final Operator operator;
    private final int major;
    private final int minor;
    private final int patch;

    private VersionRequirement(Operator operator, int major, int minor, int patch) {
        this.operator = operator;
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /** 解析（畸形 fail-fast）："*" / "^1.2.3" / "~1.2" / ">=1.0" / ">0.9" / "1.4.2"。 */
    public static VersionRequirement parse(String spec) {
        if (spec == null || spec.isBlank()) {
            throw new IllegalArgumentException("spec 不能为空");
        }
        String s = spec.trim();
        if ("*".equals(s)) {
            return new VersionRequirement(Operator.ANY, 0, 0, 0);
        }
        if (s.startsWith("^")) {
            int[] v = parseTriple(s.substring(1));
            return new VersionRequirement(Operator.CARET, v[0], v[1], v[2]);
        }
        if (s.startsWith("~")) {
            int[] v = parseTriple(s.substring(1));
            return new VersionRequirement(Operator.TILDE, v[0], v[1], v[2]);
        }
        if (s.startsWith(">=")) {
            int[] v = parseTriple(s.substring(2));
            return new VersionRequirement(Operator.GTE, v[0], v[1], v[2]);
        }
        if (s.startsWith(">")) {
            int[] v = parseTriple(s.substring(1));
            return new VersionRequirement(Operator.GT, v[0], v[1], v[2]);
        }
        int[] v = parseTriple(s);
        return new VersionRequirement(Operator.EXACT, v[0], v[1], v[2]);
    }

    /** 实际版本满足要求否（点分缺段补 0；-prerelease 后缀低于同基段）。 */
    public boolean satisfies(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version 不能为空");
        }
        if (operator == Operator.ANY) {
            return true;
        }
        boolean suffix = version.trim().indexOf('-') >= 0;
        int[] v = parseTriple(version);
        int cmp = compareTriple(v[0], v[1], v[2], suffix);
        return switch (operator) {
            case GTE -> cmp >= 0;
            case GT -> cmp > 0;
            case EXACT -> cmp == 0;
            case TILDE -> cmp >= 0 && compareTriple(v[0], v[1], v[2], suffix,
                    major, minor + 1, 0) < 0;
            case CARET -> cmp >= 0 && caretUpperOk(v, suffix);
            default -> false;
        };
    }

    /** CARET 上界判定：1.x → <2.0.0；0.2.x → <0.3.0；0.0.3 → <0.0.4。 */
    private boolean caretUpperOk(int[] v, boolean suffix) {
        int upperMajor;
        int upperMinor;
        int upperPatch;
        if (major > 0) {
            upperMajor = major + 1;
            upperMinor = 0;
            upperPatch = 0;
        } else if (minor > 0) {
            upperMajor = 0;
            upperMinor = minor + 1;
            upperPatch = 0;
        } else {
            upperMajor = 0;
            upperMinor = 0;
            upperPatch = patch + 1;
        }
        return compareTriple(v[0], v[1], v[2], suffix, upperMajor, upperMinor, upperPatch) < 0;
    }

    /** 实际版本（含 prerelease 标记）与要求基线比较。 */
    private int compareTriple(int vm, int vn, int vp, boolean suffix) {
        return compareTriple(vm, vn, vp, suffix, major, minor, patch);
    }

    /** 三段比较（短补 0 语义由解析保证；prerelease 低于同基段 release）。 */
    private static int compareTriple(int vm, int vn, int vp, boolean suffix,
                                     int a, int b, int c) {
        if (vm != a) {
            return Integer.compare(vm, a);
        }
        if (vn != b) {
            return Integer.compare(vn, b);
        }
        if (vp != c) {
            return Integer.compare(vp, c);
        }
        return suffix ? -1 : 0;
    }

    private static int[] parseTriple(String s) {
        String[] parts = s.trim().split("-", 2)[0].split("\\.");
        return new int[]{part(parts, 0), part(parts, 1), part(parts, 2)};
    }

    private static int part(String[] parts, int idx) {
        if (idx >= parts.length || parts[idx].isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[idx].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("版本段须为数字：" + parts[idx]);
        }
    }

    /** 算子读数（解析面）。 */
    public Operator operator() {
        return operator;
    }

    @Override
    public String toString() {
        return operator.name().toLowerCase(Locale.ROOT) + " " + major + "." + minor + "." + patch;
    }
}
