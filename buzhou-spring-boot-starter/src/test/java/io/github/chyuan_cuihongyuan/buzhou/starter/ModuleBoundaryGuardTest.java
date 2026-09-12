package io.github.chyuan_cuihongyuan.buzhou.starter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 模块边界守卫（spec 707 / T965，ArchUnit / eslint no-restricted-imports 思想
 * 自写实现——零第三方依赖）：源码级扫描全部 buzhou 模块 src/main/java，
 * 物理校验两条文档既有约定——
 * <ol>
 *   <li><b>internal 跨模块禁止引用</b>（spec 09 包级约定：internal 不承诺兼容，
 *       跨模块引用 = 意外承担破坏性升级）；</li>
 *   <li><b>feature 模块互相禁止直接依赖</b>（spec 09 星形拓扑：跨机制协作走
 *       core 事件总线或 core SPI；唯一二层边 otel/dashboard → observability）。</li>
 * </ol>
 * 归属判定：包 → 模块 longest-prefix 映射（各模块自身源码包全集自举）；FQN
 * 内联使用与 import 同等扫描。
 */
class ModuleBoundaryGuardTest {

    private static final Path REPO_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final String BASE = "io.github.chyuan_cuihongyuan.buzhou";
    private static final Pattern BUZHOU_FQN = Pattern.compile(
            BASE.replaceAll("\\.", "\\\\.") + "\\.[\\w.]+");
    private static final Pattern IMPORT = Pattern.compile(
            "^\\s*import\\s+(?:static\\s+)?(" + BASE.replaceAll("\\.", "\\\\.") + "\\.[\\w.]+)\\s*;");

    @Test
    void noCrossModuleInternalOrFeatureToFeatureReferences() throws IOException {
        // 仓库布局门：surefire cwd 非模块目录（如 IDE 根目录跑）时跳过——诚实边界
        assumeTrue(Files.isDirectory(REPO_ROOT.resolve("buzhou-core/src/main/java")),
                "非仓库布局（找不到 buzhou-core/src/main/java）——边界扫描跳过");

        Map<String, List<String>> modulePackages = new HashMap<>();
        try (Stream<Path> mods = Files.list(REPO_ROOT)) {
            mods.filter(p -> p.getFileName().toString().startsWith("buzhou-"))
                    .filter(p -> Files.isDirectory(p.resolve("src/main/java")))
                    .forEach(mod -> collectPackages(mod, modulePackages));
        }

        List<String> violations = new ArrayList<>();
        try (Stream<Path> mods = Files.list(REPO_ROOT)) {
            mods.filter(p -> p.getFileName().toString().startsWith("buzhou-"))
                    .filter(p -> Files.isDirectory(p.resolve("src/main/java")))
                    .forEach(mod -> scanModule(mod, modulePackages, violations));
        }

        assertThat(violations)
                .as("模块边界违规——internal 跨模块引用 / feature 互依均为文档明令禁止"
                        + "（spec 09）；存量治理：迁出 internal 或改走 core SPI/事件总线")
                .isEmpty();
    }

    private static void collectPackages(Path module, Map<String, List<String>> out) {
        String name = module.getFileName().toString();
        List<String> packages = out.computeIfAbsent(name, k -> new ArrayList<>());
        Path src = module.resolve("src/main/java");
        try (Stream<Path> dirs = Files.walk(src)) {
            dirs.filter(Files::isDirectory)
                    .map(src::relativize)
                    .map(p -> p.toString().replace('\\', '.').replace('/', '.'))
                    .filter(p -> !p.equals(""))
                    .forEach(packages::add);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void scanModule(Path module, Map<String, List<String>> modulePackages,
                                   List<String> violations) {
        String selfModule = module.getFileName().toString();
        Path src = module.resolve("src/main/java");
        try (Stream<Path> files = Files.walk(src)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(file -> {
                String content = read(file);
                String selfPackage = selfPackageOf(src, file);
                List<String> fqns = referencedFqns(content);
                for (String fqn : fqns) {
                    String target = ownerOf(fqn, modulePackages);
                    if (target == null || target.equals(selfModule)) {
                        continue;
                    }
                    String imported = importOf(content, fqn);
                    if (imported != null && imported.contains(".internal.")) {
                        violations.add("internal 跨模块引用：" + selfModule + " (" + selfPackage
                                + ") → " + target + " 的 " + fqn);
                    } else if (imported == null && fqn.contains(".internal.")) {
                        violations.add("internal 跨模块引用（内联 FQN）：" + selfModule + " → "
                                + target + " 的 " + fqn);
                    } else if (imported != null && isFeatureToFeature(selfModule, target)) {
                        violations.add("feature 互依：" + selfModule + " → " + target + " 的 " + fqn);
                    }
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** feature 互依判定：双方都不是 core，且不在唯一二层边（otel/dashboard → observability）内。 */
    private static boolean isFeatureToFeature(String source, String target) {
        if (source.equals("buzhou-core") || target.equals("buzhou-core")) {
            return false;
        }
        if ((source.equals("buzhou-observe-otel") || source.equals("buzhou-observe-dashboard"))
                && target.equals("buzhou-observability")) {
            return false;
        }
        return true;
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String selfPackageOf(Path srcRoot, Path file) {
        return srcRoot.relativize(file.getParent()).toString()
                .replace('\\', '.').replace('/', '.');
    }

    /** 文件中实际出现的 buzhou FQN（import + 内联），去重。 */
    private static List<String> referencedFqns(String content) {
        List<String> out = new ArrayList<>();
        for (String line : content.split("\n")) {
            Matcher imp = IMPORT.matcher(line);
            if (imp.find()) {
                out.add(imp.group(1));
                continue;
            }
            Matcher fqn = BUZHOU_FQN.matcher(line);
            while (fqn.find()) {
                out.add(fqn.group());
            }
        }
        return out.stream().distinct().toList();
    }

    /** import 语句原文（该 FQN 以 import 形式出现时），否则 null（内联 FQN）。 */
    private static String importOf(String content, String fqn) {
        Pattern p = Pattern.compile("^\\s*import\\s+(?:static\\s+)?"
                + Pattern.quote(fqn) + "\\s*;", Pattern.MULTILINE);
        Matcher m = p.matcher(content);
        return m.find() ? fqn : null;
    }

    /** FQN → 拥有该包的模块（longest-prefix；无归属返回 null——第三方/基础包）。 */
    private static String ownerOf(String fqn, Map<String, List<String>> modulePackages) {
        String best = null;
        int bestLen = -1;
        for (Map.Entry<String, List<String>> e : modulePackages.entrySet()) {
            for (String pkg : e.getValue()) {
                if ((fqn.equals(pkg) || fqn.startsWith(pkg + ".")) && pkg.length() > bestLen) {
                    best = e.getKey();
                    bestLen = pkg.length();
                }
            }
        }
        return best;
    }
}
