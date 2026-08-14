package io.github.chyuan_cuihongyuan.buzhou.skill.classpath;

import io.github.chyuan_cuihongyuan.buzhou.skill.Skill;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillResource;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * classpath 内置 Skill 扫描器（spec 04）。
 *
 * <p>扫描 {@code META-INF/skills/<name>/SKILL.md}（对齐 Claude Code 目录布局）及同目录资源，
 * 解析 frontmatter + 正文，产出 {@link ClasspathSkillEntry}。jar 内置 Skill 引依赖即得。
 *
 * <p>资源（脚本/模板等文本）随扫描一并读入内存（classpath 资源有界、随 jar 分发）；
 * {@code loadResource} 时直返，超大走 spill 管道（同 02 号档）。
 */
public class ClasspathSkillScanner {

    private static final System.Logger LOGGER = System.getLogger(ClasspathSkillScanner.class.getName());

    public static final String DEFAULT_LOCATION = "classpath*:META-INF/skills/";
    private static final String SKILL_MD = "SKILL.md";

    private final List<String> locations;
    private final PathMatchingResourcePatternResolver resolver;

    public ClasspathSkillScanner() {
        this(List.of(DEFAULT_LOCATION));
    }

    public ClasspathSkillScanner(List<String> locations) {
        this.locations = locations == null || locations.isEmpty()
                ? List.of(DEFAULT_LOCATION) : List.copyOf(locations);
        this.resolver = new PathMatchingResourcePatternResolver();
    }

    /** 扫描全部 location，按 skill name 聚合；同名以先扫到者为准。 */
    public Map<String, ClasspathSkillEntry> scan() {
        Map<String, ClasspathSkillEntry> result = new LinkedHashMap<>();
        for (String location : locations) {
            String pathBase = stripPrefix(location);
            String pattern = location.endsWith("/") ? location + "**" : location + "/**";
            try {
                Resource[] resources = resolver.getResources(pattern);
                Map<String, List<Resource>> bySkillDir = groupBySkillDir(resources, pathBase);
                for (Map.Entry<String, List<Resource>> entry : bySkillDir.entrySet()) {
                    ClasspathSkillEntry parsed = parseSkillDir(entry.getKey(), entry.getValue(), pathBase);
                    if (parsed != null) {
                        result.putIfAbsent(parsed.skill().name(), parsed);
                    }
                }
            } catch (IOException e) {
                // impl-51：扫描失败不阻断启动，但必须可见（此前技能静默消失、排障不可诊断）
                LOGGER.log(System.Logger.Level.WARNING,
                        "classpath 技能扫描失败（" + location + "）：" + e.getMessage());
            }
        }
        return Map.copyOf(result);
    }

    private Map<String, List<Resource>> groupBySkillDir(Resource[] resources, String pathBase) {
        Map<String, List<Resource>> grouped = new LinkedHashMap<>();
        for (Resource resource : resources) {
            String relative = relativePath(resource, pathBase);
            if (relative == null || relative.isEmpty()) {
                continue;
            }
            int slash = relative.indexOf('/');
            if (slash < 0) {
                continue; // 直接位于 skills 根下的散落文件，忽略
            }
            String skillDir = relative.substring(0, slash);
            grouped.computeIfAbsent(skillDir, k -> new ArrayList<>()).add(resource);
        }
        return grouped;
    }

    private ClasspathSkillEntry parseSkillDir(String dirName, List<Resource> files, String pathBase) {
        Resource skillMd = null;
        for (Resource r : files) {
            String rel = relativePath(r, pathBase);
            if (rel != null && rel.endsWith("/" + SKILL_MD)) {
                skillMd = r;
                break;
            }
        }
        if (skillMd == null) {
            return null;
        }
        try {
            String content = readAsString(skillMd);
            ParsedSkillMd parsed = ParsedSkillMd.parse(content);
            String name = parsed.frontmatter().name().isBlank() ? dirName : parsed.frontmatter().name();
            List<SkillResource> resourceMeta = new ArrayList<>();
            Map<String, String> resourceContents = new LinkedHashMap<>();
            for (Resource r : files) {
                String rel = relativePath(r, pathBase);
                // 跳过目录资源（classpath*:** 会返回目录本身，URL 以 / 结尾）与 SKILL.md
                if (rel == null || rel.endsWith("/") || rel.endsWith("/" + SKILL_MD)) {
                    continue;
                }
                String relPath = rel.substring(rel.indexOf('/') + 1);
                if (relPath.isEmpty()) {
                    continue;
                }
                String text = readAsString(r);
                resourceContents.put(relPath, text);
                resourceMeta.add(new SkillResource(relPath, text.length(), inferMediaType(relPath)));
            }
            Skill skill = new Skill(name, parsed.frontmatter().description(),
                    parsed.frontmatter().allowedTools(), parsed.body(), List.copyOf(resourceMeta),
                    SkillSource.CLASSPATH);
            return new ClasspathSkillEntry(skill, resourceContents);
        } catch (IOException e) {
            // impl-51：SKILL.md 读/解析失败记 WARN——技能不再静默消失
            LOGGER.log(System.Logger.Level.WARNING,
                    "技能目录解析失败（" + dirName + "）：" + e.getMessage());
            return null;
        }
    }

    /** 取相对 skills 根的路径；解析失败返回 null。 */
    static String relativePath(Resource resource, String pathBase) {
        String url;
        try {
            url = resource.getURL().toString();
        } catch (IOException e) {
            return null;
        }
        int idx = url.indexOf(pathBase);
        if (idx < 0) {
            return null;
        }
        return url.substring(idx + pathBase.length());
    }

    private static String stripPrefix(String location) {
        String base = location;
        if (base.startsWith("classpath*:")) {
            base = base.substring("classpath*:".length());
        } else if (base.startsWith("classpath:")) {
            base = base.substring("classpath:".length());
        }
        return base;
    }

    private static String readAsString(Resource resource) throws IOException {
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String inferMediaType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".md")) {
            return "text/markdown";
        }
        if (lower.endsWith(".json")) {
            return "application/json";
        }
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) {
            return "application/yaml";
        }
        return "text/plain";
    }
}
