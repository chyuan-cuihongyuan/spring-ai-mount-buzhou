package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 评估数据集 store（spec 52 §A / T190）：{@link SessionStateStore} 合成会话
 * {@link #SESSION_ID} 上的 dataset/item 持久化（对齐 {@code WebhookOutbox} 先例——
 * {@code __buzhou.*} 合成会话不在 fsck 会话全集内，天然豁免）。
 *
 * <p>键布局：元数据 {@code eval.ds.<name>}、条目 {@code eval.ds.<name>.item.<000001>}
 * （scanByPrefix 下推复用；键序即添加序）。写入经 read-modify-write 维护元数据计数与
 * nextItemId——单进程顺序调用假设（并发 last-writer-wins，诚实入档）。
 *
 * <p>未使用评估面零影响：本类不进自动装配（宿主按需构造），写入只落合成会话。
 */
public final class EvalDatasetStore {

    /** 合成会话 Id：不进任何会话生命周期清理；fsck 天然豁免（对齐 {@code __buzhou.webhook__}）。 */
    static final String SESSION_ID = "__buzhou.eval__";

    static final String PREFIX = "eval.ds.";

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9-]{1,64}");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SessionStateStore stateStore;

    public EvalDatasetStore(SessionStateStore stateStore) {
        this.stateStore = stateStore;
    }

    /** 建数据集（重名 / 非法名 fail-fast，挂 EVAL_OPERATION_INVALID）。 */
    public EvalDatasetMeta createDataset(String name, String description) {
        requireValidName(name);
        if (dataset(name).isPresent()) {
            throw evalError("数据集已存在：" + name, "换名新建，或先 deleteDataset 再重建");
        }
        EvalDatasetMeta meta = new EvalDatasetMeta(name, blankToNull(description), 0, Instant.now());
        stateStore.put(SESSION_ID, new StateEntry(dsKey(name), encode(metaToMap(meta)),
                "eval", 0, null, meta.createdAt()));
        return meta;
    }

    /** 数据集元数据（不存在 = empty）。 */
    public Optional<EvalDatasetMeta> dataset(String name) {
        requireValidName(name);
        return stateStore.get(SESSION_ID, dsKey(name)).map(e -> decodeMeta(e.value(), name));
    }

    /** 全部数据集元数据（按名排序）。 */
    public List<EvalDatasetMeta> listDatasets() {
        List<EvalDatasetMeta> result = new ArrayList<>();
        stateStore.scanByPrefix(SESSION_ID, PREFIX).forEach((key, entry) -> {
            // 元数据键 = 前缀 + name（无 ".item." 段）；条目键跳过
            String rest = key.substring(PREFIX.length());
            if (!rest.contains(".item.")) {
                result.add(decodeMeta(entry.value(), rest));
            }
        });
        result.sort((a, b) -> a.name().compareTo(b.name()));
        return result;
    }

    /** 加评估项（dataset 必须已存在；input/expected 非空）。返回条目 Id。 */
    public String addItem(String name, String input, String expected,
            String sourceSessionId, Integer sourceTurnSeq) {
        requireText(input, "input");
        requireText(expected, "expected");
        Map<String, Object> meta = stateStore.get(SESSION_ID, dsKey(name))
                .map(e -> decodeMap(e.value()))
                .orElseThrow(evalErrorSupplier("数据集未建：" + name, "先 createDataset 再加条目"));
        // read-modify-write：nextItemId 递增 + itemCount 同步（单进程顺序假设）
        long next = ((Number) meta.getOrDefault("nextItemId", 1L)).longValue();
        String id = String.format("%06d", next);
        EvalItem item = new EvalItem(id, input, expected, blankToNull(sourceSessionId),
                sourceTurnSeq, Instant.now());
        stateStore.put(SESSION_ID, new StateEntry(itemKey(name, id), encode(itemToMap(item)),
                "eval", 0, null, item.createdAt()));
        meta.put("nextItemId", next + 1);
        meta.put("itemCount", ((Number) meta.getOrDefault("itemCount", 0)).intValue() + 1);
        stateStore.put(SESSION_ID, new StateEntry(dsKey(name), encode(meta), "eval", 0, null, Instant.now()));
        return id;
    }

    /** 数据集全部条目（键序 = 添加序）。 */
    public List<EvalItem> items(String name) {
        requireValidName(name);
        List<EvalItem> result = new ArrayList<>();
        stateStore.scanByPrefix(SESSION_ID, itemPrefix(name))
                .forEach((key, entry) -> result.add(decodeItem(entry.value(), key)));
        result.sort((a, b) -> a.id().compareTo(b.id()));
        return result;
    }

    /** 删数据集（元数据 + 条目；run 记录独立前缀不级联）。不存在 = false。 */
    public boolean deleteDataset(String name) {
        requireValidName(name);
        if (dataset(name).isEmpty()) {
            return false;
        }
        // 边界：只删 dsKey 本键与 "<name>." 后代键（防 "reg" 误删 "reg-2"）
        String self = dsKey(name);
        String child = PREFIX + name + ".";
        stateStore.scanByPrefix(SESSION_ID, self).keySet().stream()
                .filter(key -> key.equals(self) || key.startsWith(child))
                .forEach(key -> stateStore.delete(SESSION_ID, key));
        return true;
    }

    // ---- 键与编解码 ----

    private static String dsKey(String name) {
        return PREFIX + name;
    }

    private static String itemPrefix(String name) {
        return PREFIX + name + ".item.";
    }

    private static String itemKey(String name, String id) {
        return itemPrefix(name) + id;
    }

    private static Map<String, Object> decodeMap(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            throw new BuzhouException(ErrorCode.DATA_CORRUPTION, "评估数据集记录解析失败：" + e.getMessage(), e);
        }
    }

    private static String encode(Map<String, Object> map) {
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            throw new BuzhouException(ErrorCode.DATA_CORRUPTION, "评估数据集记录编码失败：" + e.getMessage(), e);
        }
    }

    private static Map<String, Object> metaToMap(EvalDatasetMeta meta) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", meta.name());
        map.put("description", meta.description());
        map.put("itemCount", meta.itemCount());
        map.put("nextItemId", 1L);
        map.put("createdAt", meta.createdAt().toString());
        return map;
    }

    private EvalDatasetMeta decodeMeta(String json, String name) {
        Map<String, Object> map = decodeMap(json);
        int itemCount = ((Number) map.getOrDefault("itemCount", 0)).intValue();
        Instant createdAt = Instant.parse(String.valueOf(map.get("createdAt")));
        return new EvalDatasetMeta(name, (String) map.get("description"), itemCount, createdAt);
    }

    private static Map<String, Object> itemToMap(EvalItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.id());
        map.put("input", item.input());
        map.put("expected", item.expected());
        if (item.sourceSessionId() != null) {
            map.put("sourceSessionId", item.sourceSessionId());
        }
        if (item.sourceTurnSeq() != null) {
            map.put("sourceTurnSeq", item.sourceTurnSeq());
        }
        map.put("createdAt", item.createdAt().toString());
        return map;
    }

    private EvalItem decodeItem(String json, String key) {
        Map<String, Object> map = decodeMap(json);
        Number turnSeq = (Number) map.get("sourceTurnSeq");
        return new EvalItem((String) map.get("id"),
                (String) map.get("input"),
                (String) map.get("expected"),
                (String) map.get("sourceSessionId"),
                turnSeq == null ? null : turnSeq.intValue(),
                Instant.parse(String.valueOf(map.get("createdAt"))));
    }

    // ---- 校验 ----

    private static void requireValidName(String name) {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw evalError("数据集名非法：" + name, "取 [a-z0-9-] 且长度 1-64，如 regression-basic");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw evalError("评估项 " + field + " 不能为空", "补全后重试");
        }
    }

    private static BuzhouException evalError(String message, String action) {
        return new BuzhouException(ErrorCode.EVAL_OPERATION_INVALID, message + "（修法：" + action + "）");
    }

    private static java.util.function.Supplier<BuzhouException> evalErrorSupplier(String message, String action) {
        return () -> evalError(message, action);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
