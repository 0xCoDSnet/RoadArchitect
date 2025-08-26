package net.oxcodsnet.roadarchitect.util;

import net.minecraft.nbt.NbtCompound;

/**
 * Small helpers for safe NBT reads to reduce boilerplate.
 */
public final class NbtUtils {
    private NbtUtils() {
    }

    /**
     * Reads an enum value from NBT. If the key is missing or the value is invalid,
     * returns the provided default.
     */
    public static <E extends Enum<E>> E getEnumOrDefault(NbtCompound tag, String key, Class<E> type, E def) {
        if (!tag.contains(key)) return def;
        String raw = tag.getString(key);
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }

    /* ===================================================================== */
    /* Long↔value list helpers (k/v fields)                                   */
    /* ===================================================================== */

    private static final String K = "k";
    private static final String V = "v";

    public static net.minecraft.nbt.NbtList toLongIntList(java.util.Map<Long, Integer> map) {
        net.minecraft.nbt.NbtList list = new net.minecraft.nbt.NbtList();
        for (java.util.Map.Entry<Long, Integer> e : map.entrySet()) {
            net.minecraft.nbt.NbtCompound elem = new net.minecraft.nbt.NbtCompound();
            elem.putLong(K, e.getKey());
            elem.putInt(V, e.getValue());
            list.add(elem);
        }
        return list;
    }

    public static void fillLongIntMap(net.minecraft.nbt.NbtList list, java.util.Map<Long, Integer> out) {
        for (int i = 0; i < list.size(); i++) {
            net.minecraft.nbt.NbtCompound elem = list.getCompound(i);
            out.put(elem.getLong(K), elem.getInt(V));
        }
    }

    public static net.minecraft.nbt.NbtList toLongDoubleList(java.util.Map<Long, Double> map) {
        net.minecraft.nbt.NbtList list = new net.minecraft.nbt.NbtList();
        for (java.util.Map.Entry<Long, Double> e : map.entrySet()) {
            net.minecraft.nbt.NbtCompound elem = new net.minecraft.nbt.NbtCompound();
            elem.putLong(K, e.getKey());
            elem.putDouble(V, e.getValue());
            list.add(elem);
        }
        return list;
    }

    public static void fillLongDoubleMap(net.minecraft.nbt.NbtList list, java.util.Map<Long, Double> out) {
        for (int i = 0; i < list.size(); i++) {
            net.minecraft.nbt.NbtCompound elem = list.getCompound(i);
            out.put(elem.getLong(K), elem.getDouble(V));
        }
    }

    public static net.minecraft.nbt.NbtList toLongStringList(java.util.Map<Long, String> map) {
        net.minecraft.nbt.NbtList list = new net.minecraft.nbt.NbtList();
        for (java.util.Map.Entry<Long, String> e : map.entrySet()) {
            net.minecraft.nbt.NbtCompound elem = new net.minecraft.nbt.NbtCompound();
            elem.putLong(K, e.getKey());
            elem.putString(V, e.getValue());
            list.add(elem);
        }
        return list;
    }

    public static void fillLongStringMap(net.minecraft.nbt.NbtList list, java.util.Map<Long, String> out) {
        for (int i = 0; i < list.size(); i++) {
            net.minecraft.nbt.NbtCompound elem = list.getCompound(i);
            out.put(elem.getLong(K), elem.getString(V));
        }
    }
}
