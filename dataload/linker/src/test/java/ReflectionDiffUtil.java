import java.lang.reflect.*;
import java.util.*;

public class ReflectionDiffUtil {

    public static List<String> diffObjects(Object a, Object b) {
        return diffObjects(a, b, "");
    }

    private static List<String> diffObjects(Object a, Object b, String path) {
        List<String> diffs = new ArrayList<>();

        // Handle nulls
        if (a == b) return diffs;
        if (a == null || b == null) {
            diffs.add(path + ": one is null, the other is not");
            return diffs;
        }

        // Handle primitive wrappers, strings, enums, etc.
        if (isSimpleType(a.getClass()) || isSimpleType(b.getClass())) {
            if (!a.equals(b)) diffs.add(path + ": '" + a + "' != '" + b + "'");
            return diffs;
        }

        // Handle collections
        if (a instanceof Collection && b instanceof Collection) {
            compareCollections((Collection<?>) a, (Collection<?>) b, path, diffs);
            return diffs;
        }

        // Handle maps
        if (a instanceof Map && b instanceof Map) {
            compareMaps((Map<?, ?>) a, (Map<?, ?>) b, path, diffs);
            return diffs;
        }

        // Different classes
        if (!a.getClass().equals(b.getClass())) {
            diffs.add(path + ": different classes (" + a.getClass().getName() + " vs " + b.getClass().getName() + ")");
            return diffs;
        }

        // Use reflection for fields
        for (Field field : a.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object valA = field.get(a);
                Object valB = field.get(b);
                diffs.addAll(diffObjects(valA, valB, path.isEmpty() ? field.getName() : path + "." + field.getName()));
            } catch (IllegalAccessException e) {
                diffs.add(path + "." + field.getName() + ": [error accessing field]");
            }
        }

        return diffs;
    }

    private static void compareCollections(Collection<?> a, Collection<?> b, String path, List<String> diffs) {
        if (a.size() != b.size()) {
            diffs.add(path + ": different collection sizes (" + a.size() + " vs " + b.size() + ")");
            return;
        }

        Iterator<?> itA = a.iterator();
        Iterator<?> itB = b.iterator();
        int index = 0;
        while (itA.hasNext() && itB.hasNext()) {
            diffs.addAll(diffObjects(itA.next(), itB.next(), path + "[" + index + "]"));
            index++;
        }
    }

    private static void compareMaps(Map<?, ?> a, Map<?, ?> b, String path, List<String> diffs) {
        if (a.size() != b.size()) {
            diffs.add(path + ": different map sizes (" + a.size() + " vs " + b.size() + ")");
        }

        Set<Object> allKeys = new HashSet<>();
        allKeys.addAll(a.keySet());
        allKeys.addAll(b.keySet());

        for (Object key : allKeys) {
            Object valA = a.get(key);
            Object valB = b.get(key);
            if (!Objects.equals(valA, valB)) {
                diffs.addAll(diffObjects(valA, valB, path + "[" + key + "]"));
            }
        }
    }

    private static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz.equals(String.class)
                || Number.class.isAssignableFrom(clazz)
                || Boolean.class.isAssignableFrom(clazz)
                || Character.class.isAssignableFrom(clazz)
                || Date.class.isAssignableFrom(clazz)
                || clazz.isEnum();
    }

    // Example usage:
    /*public static void main(String[] args) {
        // Example test
        Example a = new Example("abc", 10, List.of("x", "y"));
        Example b = new Example("abc", 11, List.of("x", "z"));
        System.out.println(diffObjects(a, b));
    }

    static class Example {
        String name;
        int value;
        List<String> tags;

        Example(String n, int v, List<String> t) {
            name = n; value = v; tags = t;
        }
    }*/
}

