package com.vetclinic.common.web;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Display labels for enum values ("DOG" → "Dog"). Done in Java with Locale.ROOT (CLAUDE.md rule 15) instead of
 * Thymeleaf's #strings.toLowerCase, which uses the request locale ("BIRD" would become "bırd" in Turkish).
 */
public final class EnumLabels {

    private EnumLabels() {
    }

    public static String of(Enum<?> value) {
        if (value == null) {
            return null;
        }
        String name = value.name().replace('_', ' ');
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }

    /** Enum name → label, in declaration order; used for select boxes. */
    public static <E extends Enum<E>> Map<String, String> options(Class<E> type) {
        Map<String, String> options = new LinkedHashMap<>();
        for (E value : type.getEnumConstants()) {
            options.put(value.name(), of(value));
        }
        return options;
    }
}
