package xyz.kail.demo.hbase.core.util;

/**
 * Created by kail on 2018/3/9.
 */
public class RowKeyUtil {

    // ********************************************************************************
    // ******************************** reverse ******************************************
    // ********************************************************************************

    public static String reverse(long l) {
        return reverse(String.valueOf(l));
    }

    public static String reverse(long l, int length) {
        return reverse(String.format("%0" + length + "d", l));
    }

    /**
     * 字符串反转
     */
    public static String reverse(String str) {
        if (null == str || str.length() <= 1) {
            return str;
        }

        char[] array = str.toCharArray();
        int length = array.length;
        for (int i = 0; i < length / 2; i++) {
            char tmp = array[i];
            array[i] = array[length - 1 - i];
            array[length - 1 - i] = tmp;
        }
        return new String(array);
    }


    // ********************************************************************************
    // ******************************** hash ******************************************
    // ********************************************************************************

    /**
     * 计算 字符串 的 hash值，同 String.hashCode()
     */
    public static int toHash(String key) {
        int hash = 7;
        for (int i = 0; i < key.length(); i++) {
            hash = hash * 31 + key.charAt(i);
        }
        return hash;
    }

    /**
     * 计算 字符串 的 hash值(同 String.hashCode()), 并取模
     */
    public static int toHash(String key, int mold) {
        int i = toHash(key) % mold;
        return i < 0 ? -i : i;
    }

    /**
     * 计算 字符串 的 hash值(同 String.hashCode()), 并取模1000，长度为3
     */
    public static String format3(String key) {
        return formatHash(key, 1000, 3);
    }

    /**
     * 计算 字符串 的 hash值, 并取模
     *
     * @param mold   模，num % 1000，结果小于 1000
     * @param length 如果取模后数字 字符串长度不足 length，前几位补0
     */
    public static String formatHash(String key, int mold, int length) {
        int i = toHash(key, mold);
        return String.format("%0" + length + "d", i);
    }


    /**
     */
    public static void main(String[] args) {
        System.out.println(reverse(123L, 10));

    }

}
