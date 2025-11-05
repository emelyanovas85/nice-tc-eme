package at.nice.tc.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonTreeMap extends LinkedHashMap<String, Object> {

    public JsonTreeMap() {
        super();
//        flat();  // нечего "выпрямлять"
    }

    public JsonTreeMap(Map<String, Object> map) {
        super(map);
        flat();
    }


    /**
     * Распрямляет вложенные Map в плоскую структуру с составными ключами
     * Только для значений типа Map, остальные типы (List, примитивы) остаются как есть
     * <p>
     * Пример:
     * {"user": {"name": "John", "age": 30}} -> {"user.name": "John", "user.age": 30}
     */
    public JsonTreeMap flat() {
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

            if (value instanceof Map<?, ?> nestedMap) {
                // Рекурсивно обрабатываем вложенные Map
                //noinspection unchecked
                flattenRecursive((Map<String, Object>) nestedMap, newKey, result);
            }
            result.put(newKey, value);
        }
    }

    /**
     * Синтаксический сахар, чтобы явно не кастить Object, возвращаемый методом get
     */
    public <T> T getAutocast(String key) {
        //noinspection unchecked
        return (T) get(key);
    }
}
