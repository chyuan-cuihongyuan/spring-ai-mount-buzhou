package io.github.chyuan_cuihongyuan.buzhou.core.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;

/**
 * 配置体检（spec 91 §A / T345，Spring Boot diagnostics / Spring Shell doctor 思想；
 * #51 插曲——本地仓库 jar 与源码配置面漂移——的直接启发）：把宿主实际配置的
 * {@code buzhou.*} 键与 classpath 已知键宇宙（各模块 additional metadata json 聚合）
 * 对照，产出结构化体检报告：
 *
 * <ul>
 *   <li><b>ERROR</b>——值域越界（metadata 声明为数值/布尔而实际值不可解析）；</li>
 *   <li><b>WARN</b>——未知键 + 编辑距离 ≤ {@value #MAX_SUGGEST_DISTANCE} 的最近邻
 *       建议（拼错键静默失效是配置类问题第一来源）；</li>
 *   <li><b>INFO</b>——已知且合法（仅计数，不刷屏）。</li>
 * </ul>
 *
 * <p>纯静态面：只读不写、不改行为；报告由装配层决定去向（启动日志 / 健康详情）。
 * 环境来源用 {@link org.springframework.core.env.EnumerablePropertySource} 聚合
 * （profiles/命令行/system 兜底序按 Environment 既有优先级——此处只枚举出现过的键）。
 */
public final class ConfigDoctor {

    /** 近邻建议的最大编辑距离（typo 检测惯用 1-2；键名长，取 2）。 */
    public static final int MAX_SUGGEST_DISTANCE = 2;

    /** 单报告发现上限（防坏键风暴刷爆日志）。 */
    public static final int MAX_FINDINGS = 64;

    /** 单条发现（级别 + 键 + 说明；message 不含值内容——值可能敏感）。 */
    public record Finding(String level, String key, String message) {
    }

    /** 体检报告（发现列表有界 + 各级计数 + 汇总单行）。 */
    public record DoctorReport(List<Finding> findings, int checkedKeys,
                               int errorCount, int warnCount, int infoCount) {

        /** 单行摘要（启动日志首行）。 */
        public String summary() {
            if (errorCount == 0 && warnCount == 0) {
                return "config-doctor OK（" + checkedKeys + " keys checked）";
            }
            return "config-doctor " + (errorCount > 0 ? "FAIL" : "WARN")
                    + "（checked=" + checkedKeys + " errors=" + errorCount
                    + " warnings=" + warnCount + "）";
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<String> knownKeys;
    private List<String[]> knownKeyTypes = List.of();

    /** 从 classpath 各模块 metadata json 聚合已知键宇宙。 */
    public ConfigDoctor() {
        this.knownKeys = loadKnownKeys();
    }

    /** 测试注入键宇宙（键 → metadata type；无类型的键不做值域校验）。 */
    ConfigDoctor(Map<String, String> knownKeysWithTypes) {
        this.knownKeys = knownKeysWithTypes.keySet().stream().sorted()
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        this.knownKeyTypes = knownKeysWithTypes.entrySet().stream()
                .map(e -> new String[]{e.getKey(), e.getValue()})
                .toList();
    }

    /** 体检给定键值面（value 为 null = 仅键存在性检查）。 */
    public DoctorReport examine(Map<String, String> configured) {
        List<Finding> findings = new ArrayList<>();
        int errors = 0;
        int warns = 0;
        int infos = 0;
        for (Map.Entry<String, String> entry : configured.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (knownKeys.contains(key)) {
                String problem = valueProblem(key, value);
                if (problem == null) {
                    infos++;
                } else {
                    errors++;
                    findings.add(new Finding("ERROR", key, problem));
                }
                continue;
            }
            String nearest = nearestKnown(key);
            warns++;
            findings.add(new Finding("WARN", key, nearest == null
                    ? "未知键（无近邻建议）"
                    : "未知键——是否想写 " + nearest + " ？"));
            if (findings.size() >= MAX_FINDINGS) {
                break;
            }
        }
        // spec 115 §A / T411：跨键矛盾规则（doctor v2——单键合法但组合矛盾/空转）
        warns += crossKeyFindings(configured, findings);
        return new DoctorReport(List.copyOf(findings), configured.size(), errors, warns, infos);
    }

    /**
     * spec 115 §A / T411：跨键矛盾规则（规则表有界显式——新规则须同步本 javadoc）：
     * <ul>
     *   <li>bulkhead 开而未配 agents —— NOOP 空转（开了但无上限可执行）；</li>
     *   <li>semantic-drift 开而 memory.enabled=false —— 挂不上（memory 未装配）；</li>
     *   <li>依赖键存在而开关未开（acquire-timeout / semantic-drift-threshold）——
     *       疑似以为已生效（静默空转）。</li>
     * </ul>
     */
    private static int crossKeyFindings(Map<String, String> configured, List<Finding> findings) {
        int added = 0;
        boolean bulkheadOn = truthy(configured.get("buzhou.bulkhead.enabled"));
        if (bulkheadOn && !configured.containsKey("buzhou.bulkhead.agents")
                && findings.size() < MAX_FINDINGS) {
            findings.add(new Finding("WARN", "buzhou.bulkhead.agents",
                    "隔离舱已开启但未配 agents 上限表——NOOP 空转（无上限可执行）"));
            added++;
        }
        if (!bulkheadOn && configured.containsKey("buzhou.bulkhead.acquire-timeout")
                && findings.size() < MAX_FINDINGS) {
            findings.add(new Finding("WARN", "buzhou.bulkhead.acquire-timeout",
                    "依赖键已配但 buzhou.bulkhead.enabled 未开——疑似以为已生效（静默空转）"));
            added++;
        }
        boolean driftOn = truthy(configured.get("buzhou.memory.semantic-drift"));
        boolean memoryOff = configured.containsKey("buzhou.memory.enabled")
                && !truthy(configured.get("buzhou.memory.enabled"));
        if (driftOn && memoryOff && findings.size() < MAX_FINDINGS) {
            findings.add(new Finding("WARN", "buzhou.memory.semantic-drift",
                    "漂移检测开启但 buzhou.memory.enabled=false——挂不上（memory 未装配）"));
            added++;
        }
        if (!driftOn && configured.containsKey("buzhou.memory.semantic-drift-threshold")
                && findings.size() < MAX_FINDINGS) {
            findings.add(new Finding("WARN", "buzhou.memory.semantic-drift-threshold",
                    "依赖键已配但 buzhou.memory.semantic-drift 未开——疑似以为已生效（静默空转）"));
            added++;
        }
        return added;
    }

    private static boolean truthy(String value) {
        return value != null && Boolean.parseBoolean(value.trim());
    }

    /** 值域校验（metadata type 面：Boolean/数值可解析性）。 */
    private String valueProblem(String key, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String type = typeOf(key);
        try {
            if (type.contains("Boolean")) {
                // Boolean.parseBoolean 对任意输入返回 false 不抛——严格白名单
                String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
                if (!normalized.equals("true") && !normalized.equals("false")) {
                    return "值不可解析为声明类型 " + type + "（值内容略——可能敏感）";
                }
            } else if (type.contains("Integer") || type.contains("Long")
                    || type.contains("int") || type.contains("long")) {
                Long.parseLong(value.trim());
            } else if (type.contains("Double") || type.contains("double")) {
                Double.parseDouble(value.trim());
            }
        } catch (RuntimeException e) {
            return "值不可解析为声明类型 " + type + "（值内容略——可能敏感）";
        }
        return null;
    }

    /** 最近邻已知键（编辑距离 ≤ 2 内取最小；无 = null）。 */
    String nearestKnown(String key) {
        String best = null;
        int bestDistance = MAX_SUGGEST_DISTANCE + 1;
        for (String candidate : knownKeys) {
            int distance = levenshtein(key, candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return bestDistance <= MAX_SUGGEST_DISTANCE ? best : null;
    }

    private String typeOf(String key) {
        // 惰性建索引不值得（体检是启动期一次性行为）；线性扫即可
        for (String[] entry : knownKeyTypes) {
            if (entry[0].equals(key)) {
                return entry[1];
            }
        }
        return "";
    }

    /** 编辑距离（两串都短——朴素 DP 足够）。 */
    static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    private List<String> loadKnownKeys() {
        List<String> keys = new ArrayList<>();
        List<String[]> withTypes = new ArrayList<>();
        try {
            ClassLoader loader = ConfigDoctor.class.getClassLoader();
            Enumeration<java.net.URL> resources =
                    loader.getResources("META-INF/additional-spring-configuration-metadata.json");
            while (resources.hasMoreElements()) {
                JsonNode root = MAPPER.readTree(resources.nextElement().openStream());
                for (JsonNode p : root.path("properties")) {
                    String name = p.path("name").asText("");
                    if (name.startsWith("buzhou.")) {
                        keys.add(name);
                        withTypes.add(new String[]{name, p.path("type").asText("")});
                    }
                }
            }
        } catch (Exception e) {
            // metadata 不可读 = 键宇宙为空（体检退化为全 WARN——诚实降级不静默装好）
        }
        keys.sort(Comparator.naturalOrder());
        knownKeyTypes = List.copyOf(withTypes);
        return keys;
    }

    /** 从 Spring Environment 聚合 buzhou.* 键值面（诊断入口）。 */
    public DoctorReport examine(org.springframework.core.env.Environment env) {
        Map<String, String> configured = new LinkedHashMap<>();
        if (env instanceof org.springframework.core.env.ConfigurableEnvironment ce) {
            for (org.springframework.core.env.PropertySource<?> source : ce.getPropertySources()) {
                if (source instanceof org.springframework.core.env.EnumerablePropertySource<?> eps) {
                    for (String name : eps.getPropertyNames()) {
                        if (name.startsWith("buzhou.") && !configured.containsKey(name)) {
                            configured.put(name, String.valueOf(eps.getProperty(name)));
                        }
                    }
                }
            }
        }
        return examine(configured);
    }
}
