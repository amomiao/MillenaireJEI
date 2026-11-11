package com.momos.millenairejeim.helper;

import com.momos.millenairejeim.util.MMLog;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 安全反射工具类（双模组共用）。
 * <p>
 * 对 Millenaire 或其他附属模组的类、字段、方法、构造器及延迟 {@link Supplier} 访问提供统一保护：
 * <ul>
 *     <li>所有反射操作均捕获 {@link Throwable}（含 {@link LinkageError} / {@link NoSuchFieldException} 等），绝不向外抛错影响游戏运行；</li>
 *     <li>同一类名与成员签名的失败仅通过 {@link MMLog#warn} 输出一次三语告警，避免日志刷屏；</li>
 *     <li>失败时返回安全默认值，保证调用方平稳降级。</li>
 * </ul>
 * </p>
 */
public final class SafeReflectionHelper {

    /** 按故障签名去重，保证每个失效反射点在单次运行会话内只输出一条 WARN 日志 */
    private static final Set<String> LOGGED_FAILURES = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, Optional<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    private SafeReflectionHelper() {}

    /**
     * 清理字段反射缓存（不清理已记录的告警去重集合，防止重载世界时重复刷屏）。
     */
    public static void clearCache() {
        FIELD_CACHE.clear();
    }

    /**
     * 安全加载目标类。
     */
    @Nullable
    public static Class<?> findClass(String className) {
        if (className == null || className.isBlank()) {
            return null;
        }
        try {
            return Class.forName(className);
        } catch (Throwable t) {
            warnOnce("class#" + className, className, "<class>", t);
            return null;
        }
    }

    /**
     * 安全读取对象实例字段原始值（沿继承链查找）。
     * 若字段合法存在且值本身为 {@code null}，不会产生误报警告；仅当字段缺失或访问异常时输出告警。
     */
    @Nullable
    public static Object readField(@Nullable Object target, String fieldName) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        Class<?> clazz = target.getClass();
        Field field = resolveField(clazz, fieldName);
        if (field == null) {
            return null;
        }
        try {
            return field.get(target);
        } catch (Throwable t) {
            warnOnce("field_get#" + clazz.getName() + "#" + fieldName, clazz.getName(), fieldName, t);
            return null;
        }
    }

    /**
     * 安全读取对象实例字段并校验期望类型。
     */
    public static <T> T readField(@Nullable Object target, String fieldName, Class<T> expectedType, T defaultValue) {
        Object raw = readField(target, fieldName);
        if (raw == null) {
            return defaultValue;
        }
        if (expectedType.isInstance(raw)) {
            return expectedType.cast(raw);
        }
        String className = target.getClass().getName();
        warnTypeMismatch("field_type#" + className + "#" + fieldName,
                className, fieldName, expectedType.getSimpleName(), raw.getClass().getName());
        return defaultValue;
    }

    /**
     * 安全读取字符串字段，失败或为空时返回 {@code ""}。
     */
    public static String readStringField(@Nullable Object target, String fieldName) {
        return readField(target, fieldName, String.class, "");
    }

    /**
     * 安全读取整数/数值字段，失败时返回默认值。
     */
    public static int readIntField(@Nullable Object target, String fieldName, int defaultValue) {
        Number num = readField(target, fieldName, Number.class, null);
        return num != null ? num.intValue() : defaultValue;
    }

    /**
     * 安全读取 {@link Supplier} 类型字段并触发 {@link Supplier#get()}，校验返回类型。
     */
    @Nullable
    public static <T> T readSupplierField(@Nullable Object target, String fieldName, Class<T> expectedType) {
        Object raw = readField(target, fieldName);
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof Supplier<?> supplier)) {
            String className = target.getClass().getName();
            warnTypeMismatch("supplier_field#" + className + "#" + fieldName,
                    className, fieldName, "Supplier", raw.getClass().getName());
            return null;
        }
        try {
            Object supplied = supplier.get();
            if (supplied == null) {
                return null;
            }
            if (expectedType.isInstance(supplied)) {
                return expectedType.cast(supplied);
            }
            String className = target.getClass().getName();
            warnTypeMismatch("supplier_val#" + className + "#" + fieldName,
                    className, fieldName + ".get()", expectedType.getSimpleName(), supplied.getClass().getName());
            return null;
        } catch (Throwable t) {
            String className = target.getClass().getName();
            warnOnce("supplier_get#" + className + "#" + fieldName, className, fieldName + ".get()", t);
            return null;
        }
    }

    /**
     * 安全读取静态字段并校验类型。
     */
    public static <T> T readStaticField(@Nullable Class<?> clazz, String fieldName, Class<T> expectedType, T defaultValue) {
        if (clazz == null || fieldName == null || fieldName.isBlank()) {
            return defaultValue;
        }
        Field field = resolveField(clazz, fieldName);
        if (field == null) {
            return defaultValue;
        }
        try {
            Object raw = field.get(null);
            if (raw == null) {
                return defaultValue;
            }
            if (expectedType.isInstance(raw)) {
                return expectedType.cast(raw);
            }
            warnTypeMismatch("static_type#" + clazz.getName() + "#" + fieldName,
                    clazz.getName(), fieldName, expectedType.getSimpleName(), raw.getClass().getName());
            return defaultValue;
        } catch (Throwable t) {
            warnOnce("static_get#" + clazz.getName() + "#" + fieldName, clazz.getName(), fieldName, t);
            return defaultValue;
        }
    }

    /**
     * 安全查找声明或继承的构造方法。
     */
    @Nullable
    public static Constructor<?> findConstructor(@Nullable Class<?> clazz, Class<?>... parameterTypes) {
        if (clazz == null) {
            return null;
        }
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor(parameterTypes);
            ctor.setAccessible(true);
            return ctor;
        } catch (Throwable t) {
            warnOnce("ctor#" + clazz.getName() + "#" + parameterTypes.length, clazz.getName(), "<init>", t);
            return null;
        }
    }

    /**
     * 安全通过构造方法创建实例。
     */
    @Nullable
    public static Object newInstance(@Nullable Constructor<?> ctor, Object... args) {
        if (ctor == null) {
            return null;
        }
        try {
            return ctor.newInstance(args);
        } catch (Throwable t) {
            String owner = ctor.getDeclaringClass().getName();
            warnOnce("new_instance#" + owner, owner, "<init>", t);
            return null;
        }
    }

    /**
     * 安全查找声明或继承链上的方法。
     */
    @Nullable
    public static Method findMethod(@Nullable Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        Class<?> current = clazz;
        Throwable lastError = null;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                lastError = e;
                current = current.getSuperclass();
            } catch (Throwable t) {
                warnOnce("method_find#" + clazz.getName() + "#" + methodName, clazz.getName(), methodName + "()", t);
                return null;
            }
        }
        warnOnce("method_find#" + clazz.getName() + "#" + methodName, clazz.getName(), methodName + "()",
                lastError != null ? lastError : new NoSuchMethodException(methodName));
        return null;
    }

    /**
     * 安全调用已解析的实例或静态方法（静态方法时 {@code target} 传 {@code null}）。
     */
    @Nullable
    public static Object invokeMethod(@Nullable Object target, @Nullable Method method, Object... args) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target, args);
        } catch (Throwable t) {
            String owner = method.getDeclaringClass().getName();
            warnOnce("method_invoke#" + owner + "#" + method.getName(), owner, method.getName() + "()", t);
            return null;
        }
    }

    /**
     * 安全按名称查找并调用无参或指定参数方法。
     */
    @Nullable
    public static <T> T invokeMethod(
            @Nullable Object target,
            String methodName,
            Class<T> expectedType,
            T defaultValue,
            Class<?>[] paramTypes,
            Object... args
    ) {
        if (target == null) {
            return defaultValue;
        }
        Method method = findMethod(target.getClass(), methodName, paramTypes);
        if (method == null) {
            return defaultValue;
        }
        Object res = invokeMethod(target, method, args);
        if (res == null) {
            return defaultValue;
        }
        if (expectedType.isInstance(res)) {
            return expectedType.cast(res);
        }
        String className = target.getClass().getName();
        warnTypeMismatch("method_ret#" + className + "#" + methodName,
                className, methodName + "()", expectedType.getSimpleName(), res.getClass().getName());
        return defaultValue;
    }

    /**
     * 安全按名称调用无参方法并校验返回类型。
     */
    public static <T> T invokeNoArgMethod(@Nullable Object target, String methodName, Class<T> expectedType, T defaultValue) {
        return invokeMethod(target, methodName, expectedType, defaultValue, new Class<?>[0]);
    }

    @Nullable
    private static Field resolveField(Class<?> clazz, String fieldName) {
        String cacheKey = clazz.getName() + "#" + fieldName;
        Optional<Field> cached = FIELD_CACHE.get(cacheKey);
        if (cached != null) {
            return cached.orElse(null);
        }
        Class<?> current = clazz;
        Throwable lastError = null;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                FIELD_CACHE.put(cacheKey, Optional.of(field));
                return field;
            } catch (NoSuchFieldException e) {
                lastError = e;
                current = current.getSuperclass();
            } catch (Throwable t) {
                FIELD_CACHE.put(cacheKey, Optional.empty());
                warnOnce("field_find#" + cacheKey, clazz.getName(), fieldName, t);
                return null;
            }
        }
        FIELD_CACHE.put(cacheKey, Optional.empty());
        warnOnce("field_find#" + cacheKey, clazz.getName(), fieldName,
                lastError != null ? lastError : new NoSuchFieldException(fieldName));
        return null;
    }

    private static void warnOnce(String dedupKey, String targetClass, String member, Throwable cause) {
        if (!LOGGED_FAILURES.add(dedupKey)) {
            return;
        }
        String reason = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        MMLog.warn(
                "[安全反射降级] 访问 {}.{} 失败，已安全跳过并采用默认值（{}）",
                "[SafeReflection Fallback] Failed accessing {}.{}, safely using fallback ({})",
                "[Repli Réflexion Sécurisée] Échec d'accès à {}.{}, valeur par défaut utilisée ({})",
                targetClass, member, reason
        );
    }

    private static void warnTypeMismatch(
            String dedupKey, String targetClass, String member, String expected, String actual
    ) {
        if (!LOGGED_FAILURES.add(dedupKey)) {
            return;
        }
        MMLog.warn(
                "[安全反射降级] {}.{} 返回类型不匹配（期望: {}，实际: {}），已安全忽略",
                "[SafeReflection Fallback] {}.{} type mismatch (expected: {}, actual: {}), safely ignored",
                "[Repli Réflexion Sécurisée] {}.{} type incompatible (attendu: {}, réel: {}), ignoré",
                targetClass, member, expected, actual
        );
    }
}
