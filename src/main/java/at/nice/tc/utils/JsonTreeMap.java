package at.nice.tc.utils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonTreeMap extends LinkedHashMap<String, Object> {

    public JsonTreeMap() {
        super();
        flat();
    }

    public JsonTreeMap(Map<String, Object> map) {
        super(map);
        flat();
    }


    @Override
    public Object put(String k, Object v) {
        final String[] keys = k.split("\\.");
        Object current = this;
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            @SuppressWarnings("unchecked")
            Map<String, Object> currentMap = (Map<String, Object>) current;
            current = currentMap.get(key);
            if (current == null) {
                throw new RuntimeException("Нет объекта по ключу " + Arrays.toString(keys) + " [" + i + "]");
            }
        }
        //noinspection unchecked
        ((Map<String, Object>) current).put(k, v);
        return super.put(k, v);
    }

    /**
     * Распрямляет вложенные Map в плоскую структуру с составными ключами
     * Только для значений типа Map, остальные типы (List, примитивы) остаются как есть
     *
     * Пример:
     * {"user": {"name": "John", "age": 30}} -> {"user.name": "John", "user.age": 30}
     */
    public Map<String, Object> flat() {
        Map<String, Object> result = new LinkedHashMap<>();
        flattenRecursive(this, "", result);
        putAll(result);
        return this;
    }

    protected void flattenRecursive(Map<String, Object> current, String prefix, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : current.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String newKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (value instanceof Map<?,?> nestedMap) {
                // Рекурсивно обрабатываем вложенные Map
                //noinspection unchecked
                flattenRecursive((Map<String, Object>) nestedMap, newKey, result);
            } else {
                // Все остальные типы добавляем как есть
                result.put(newKey, value);
            }
        }
    }

    public <T> T getAutocast(String key) {
        //noinspection unchecked
        return (T) get(key);
    }
}
