package net.oxcodsnet.roadarchitect.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtByte;

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
        String raw = tag.getString(key, "");
        try {
            return raw.isEmpty() ? def : Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }

    /* ===================================================================== */
    /* Primitive array helpers                                               */
    /* ===================================================================== */

    /**
     * Serializes a double array into an {@link NbtList} of {@link NbtDouble}.
     */
    public static NbtList toDoubleList(double[] values) {
        NbtList list = new NbtList();
        if (values == null) return list;
        for (double v : values) list.add(NbtDouble.of(v));
        return list;
    }

    /**
     * Deserializes a list of numbers (double or string) into a double array.
     * Accepts {@link NbtDouble} or {@link net.minecraft.nbt.NbtString} elements.
     */
    public static double[] readDoubleList(NbtList list) {
        if (list == null || list.isEmpty()) return new double[0];
        double[] out = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            NbtElement el = list.get(i);
            if (el instanceof NbtDouble d) {
                out[i] = d.doubleValue();
            } else {
                // Unknown/legacy element type — default to 0.0 (will be recomputed later)
                out[i] = 0.0;
            }
        }
        return out;
    }

    /**
     * Serializes a byte array into an {@link NbtList} of {@link NbtByte}.
     */
    public static NbtList toByteList(byte[] values) {
        NbtList list = new NbtList();
        if (values == null) return list;
        for (byte b : values) list.add(NbtByte.of(b));
        return list;
    }

    /**
     * Deserializes a list into a byte array. Accepts {@link NbtByte} or
     * string-encoded numbers for forward/legacy compatibility.
     */
    public static byte[] readByteList(NbtList list) {
        if (list == null || list.isEmpty()) return new byte[0];
        byte[] out = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            NbtElement el = list.get(i);
            if (el instanceof NbtByte b) {
                out[i] = b.byteValue();
            } else {
                // Unknown/legacy element type — default to 0
                out[i] = 0;
            }
        }
        return out;
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
            net.minecraft.nbt.NbtCompound elem = list.getCompoundOrEmpty(i);
            out.put(elem.getLong(K, 0L), elem.getInt(V, 0));
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
            net.minecraft.nbt.NbtCompound elem = list.getCompoundOrEmpty(i);
            out.put(elem.getLong(K, 0L), elem.getDouble(V, 0.0));
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
            net.minecraft.nbt.NbtCompound elem = list.getCompoundOrEmpty(i);
            out.put(elem.getLong(K, 0L), elem.getString(V, ""));
        }
    }
}
