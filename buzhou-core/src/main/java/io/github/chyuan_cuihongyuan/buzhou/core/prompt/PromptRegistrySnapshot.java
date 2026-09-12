package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 提示词注册表快照导出/导入（spec 545 / T843——401 注册表扩散；Langfuse
 * export/import 思想）：全部名称的版本史 + 标签指针 → 可移植 JSON；导入
 * 到**全新空注册表**按旧版本序重放（publish 单调版本 → 新版本号与旧一致）
 * + 标签重指。
 *
 * <p>诚实边界：只支持导入到空注册表（非空即 IllegalArgumentException——
 * 合并语义复杂度不抵收益，备份/还原场景足用）；publishedAt 不保真
 * （重放时刻为准——版本号才是复现锚）。
 */
public final class PromptRegistrySnapshot {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FORMAT = "buzhou.prompt-registry-snapshot";
    private static final int VERSION = 1;

    private PromptRegistrySnapshot() {
    }

    /** 全量导出（names 字典序；版本升序；labels 全量指针）。 */
    public static String export(PromptRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("registry 必须非空");
        }
        ObjectNode root = MAPPER.createObjectNode();
        root.put("format", FORMAT);
        root.put("version", VERSION);
        ObjectNode names = root.putObject("names");
        for (String name : new java.util.TreeSet<>(registry.names())) {
            ObjectNode nameNode = names.putObject(name);
            ArrayNode versions = nameNode.putArray("versions");
            for (PromptVersion v : registry.versions(name)) {
                ObjectNode vnode = versions.addObject();
                vnode.put("version", v.version());
                vnode.put("body", v.body());
                vnode.put("note", v.note());
            }
            ObjectNode labels = nameNode.putObject("labels");
            registry.labels(name).forEach(labels::put);
        }
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("快照序列化失败", e);
        }
    }

    /**
     * 导入到**空注册表**（按旧版本序重放 publish → 新版本号与旧一致；标签
     * 重指映射版本）。非空注册表 / 格式不符 → IllegalArgumentException。
     */
    public static void importInto(PromptRegistry registry, String json) {
        JsonNode root;
        try {
            root = MAPPER.readTree(json == null ? "" : json);
        } catch (Exception e) {
            throw new IllegalArgumentException("快照 JSON 非法", e);
        }
        if (!FORMAT.equals(root.path("format").asText())) {
            throw new IllegalArgumentException("非本格式快照（format 缺失或不符）");
        }
        if (!registry.names().isEmpty()) {
            throw new IllegalArgumentException(
                    "导入目标注册表非空（快照导入仅支持空注册表——备份/还原场景）");
        }
        JsonNode names = root.path("names");
        List<String> nameOrder = new ArrayList<>();
        names.fieldNames().forEachRemaining(nameOrder::add);
        // 先重放全部版本（保持全局发布序：按名称字典序逐名重放其版本序列）
        for (String name : nameOrder) {
            JsonNode nameNode = names.get(name);
            for (JsonNode v : nameNode.path("versions")) {
                registry.publish(name, v.path("body").asText(), v.path("note").asText(null));
            }
        }
        // 再重指标签（映射回重放后的版本号——空注册表重放后版本号与旧一致）
        for (String name : nameOrder) {
            JsonNode labels = names.get(name).path("labels");
            labels.fieldNames().forEachRemaining(label -> {
                int version = labels.get(label).asInt();
                if (!"latest".equals(label)) {
                    registry.label(name, label, version);
                }
            });
        }
    }
}
