package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 技能发布通道解析（spec 813 / T1127，pnpm/yarn dist-tag 借鉴——同一包多
 * 通道标签并存、{@code latest} 指最高版本）：对 (技能名 → 通道 → 版本)
 * 注册表做<b>纯解析</b>——{@code resolve(name, channel)} 显式通道取标定版本，
 * 未标定通道回退 {@code latest}（全注册表最高版本）。
 *
 * <p>版本比较：点分整数段逐段比较（{@code 0.10.0 > 0.9.0}——字典序陷阱免疫），
 * 非数字段按字典序兜底；段数不齐短者补 0。解析只读——版本写入/审核归
 * SkillStore 写面。空通道/空名归一 {@code latest}/忽略。
 */
public final class SkillChannelResolver {

    /** 默认通道名（最高版本锚）。 */
    public static final String LATEST = "latest";

    /** 注册表条目。 */
    public record Entry(String name, String version, String channel) {
    }

    private final Map<String, Map<String, String>> byName;

    public SkillChannelResolver(List<Entry> entries) {
        Objects.requireNonNull(entries, "entries");
        Map<String, Map<String, String>> build = new HashMap<>();
        for (Entry e : entries) {
            if (e == null || e.name() == null || e.name().isBlank()
                    || e.version() == null || e.version().isBlank()) {
                continue; // 脏条目跳过
            }
            String channel = e.channel() == null || e.channel().isBlank() ? LATEST : e.channel();
            String previous = build
                    .computeIfAbsent(e.name(), k -> new HashMap<>())
                    .put(channel, e.version());
            if (previous != null && compareVersions(previous, e.version()) > 0) {
                // 同通道重复标定：保留更高版本（幂等收敛口径）
                build.get(e.name()).put(channel, previous);
            }
        }
        this.byName = Map.copyOf(build);
    }

    /**
     * 解析技能在指定通道的版本：显式标定优先；未标定通道回退 latest；
     * 技能未知返回空。
     */
    public Optional<String> resolve(String name, String channel) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String ch = channel == null || channel.isBlank() ? LATEST : channel;
        Map<String, String> channels = byName.get(name);
        if (channels == null) {
            return Optional.empty();
        }
        String tagged = channels.get(ch);
        if (tagged != null) {
            return Optional.of(tagged);
        }
        return channels.containsKey(LATEST) ? Optional.of(channels.get(LATEST))
                : Optional.ofNullable(highestVersion(channels));
    }

    /** 指定技能的全部通道标定（只读；无则空 map）。 */
    public Map<String, String> channelsOf(String name) {
        Map<String, String> channels = name == null ? null : byName.get(name);
        return channels == null ? Map.of() : Map.copyOf(channels);
    }

    /** 已注册技能名（典序只读）。 */
    public List<String> names() {
        return byName.keySet().stream().sorted().toList();
    }

    /** 点分版本比较：数字段逐段比（短补 0）；prerelease 后缀段低于同基段（semver 语义）。 */
    static int compareVersions(String a, String b) {
        String[] sa = a.split("\\.");
        String[] sb = b.split("\\.");
        int len = Math.max(sa.length, sb.length);
        for (int i = 0; i < len; i++) {
            int cmp = compareSegment(i < sa.length ? sa[i] : "0", i < sb.length ? sb[i] : "0");
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private static int compareSegment(String a, String b) {
        Long na = tryParse(a);
        Long nb = tryParse(b);
        if (na != null && nb != null) {
            return Long.compare(na, nb);
        }
        long pa = numPrefix(a);
        long pb = numPrefix(b);
        if (pa != pb) {
            return Long.compare(pa, pb);
        }
        boolean suffixA = !suffixOf(a).isEmpty();
        boolean suffixB = !suffixOf(b).isEmpty();
        if (suffixA != suffixB) {
            return suffixA ? -1 : 1; // 0-beta < 0（semver prerelease 低于 release）
        }
        return a.compareTo(b);
    }

    private static Long tryParse(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long numPrefix(String s) {
        int i = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
        }
        return i == 0 ? 0 : Long.parseLong(s.substring(0, i));
    }

    private static String suffixOf(String s) {
        int i = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
        }
        return s.substring(i);
    }

    private static String highestVersion(Map<String, String> channels) {
        return channels.values().stream()
                .max(SkillChannelResolver::compareVersions)
                .orElse(null);
    }
}
