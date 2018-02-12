package org.exemodel.util;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by zp on 16/9/21.
 */
public class BinaryUtil {


    public static final byte[] getBytes(Object o) {
        if (o == null) {
            return new byte[0];
        }

        if (o.getClass() == Long.class) {
            return toBytes((long) o);
        } else if (o instanceof Long[] || o instanceof long[]) {
            return toBytes((long[]) o);
        } else if (o instanceof byte[] || o instanceof Byte[]) {
            return toBytes((byte[]) o);
        } else if (o instanceof Integer) {
            return toBytes((int) o);
        } else if (o instanceof Integer[] || o instanceof int[]) {
            return toBytes((int[]) o);
        } else if (o instanceof Float) {
            return toBytes((float) o);
        } else if (o instanceof Float[] || o instanceof float[]) {
            return toBytes((float[]) o);
        } else if (o instanceof Double) {
            return toBytes((double) o);
        } else if (o instanceof Double[] || o instanceof double[]) {
            return toBytes((double[]) o);
        } else if (o instanceof Short) {
            return toBytes((short) o);
        } else if (o instanceof Short[] || o instanceof short[]) {
            return toBytes((short[]) o);
        } else if (o instanceof Boolean) {
            return toBytes((boolean) o);
        } else if (o instanceof Boolean[] || o instanceof boolean[]) {
            return toBytes((boolean[]) o);
        } else if (o instanceof Character) {
            return toBytes((char) o);
        } else if (o instanceof Character[] || o instanceof char[]) {
            return toBytes((char[]) o);
        } else if (o instanceof String) {
            return toBytes((String) o);
        } else if (o instanceof String[]) {
            return toBytes((String[]) o);
        } else if (o instanceof Date) {
            return toBytes((Date) o);
        } else if (o instanceof InputStream) {
            return toBytes((InputStream) o);
        } else if( o instanceof BigDecimal){
            return toBytes((BigDecimal) o);
        } else if( o instanceof BigInteger){
            return toBytes((BigInteger) o);
        } else {
            throw new RuntimeException("Error: The parameter " + o + " is not basic type!");
        }
    }


    public static final Object getValue(byte[] bytes, Class<?> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        if (clazz == Long.class || clazz == long.class) {
            return toLong(bytes);
        } else if (clazz == Integer.class || clazz == int.class) {
            return toInt(bytes);
        } else if (clazz == Float.class || clazz == float.class) {
            return toFloat(bytes);
        } else if (clazz == Double.class || clazz == double.class) {
            return toDouble(bytes);
        } else if (clazz == Short.class || clazz == short.class) {
            return toShort(bytes);
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return toBoolean(bytes);
        } else if (clazz == Character.class || clazz == char.class) {
            return toChar(bytes);
        } else if (clazz == String.class) {
            return toString(bytes);
        } else if (clazz == Date.class) {
            return toDate(bytes);
        } else if (clazz == Timestamp.class) {
            return toTimestamp(bytes);
        } else if (clazz == Long[].class || clazz == long[].class) {
            return toLongArray(bytes);
        } else if (clazz == Integer[].class || clazz == int[].class) {
            return toIntArray(bytes);
        } else if (clazz == Float[].class || clazz == float[].class) {
            return toFloatArray(bytes);
        } else if (clazz == Double[].class || clazz == double[].class) {
            return toDoubleArray(bytes);
        } else if (clazz == Short[].class || clazz == short[].class) {
            return toShortArray(bytes);
        } else if (clazz == Boolean[].class || clazz == boolean[].class) {
            return toBooleanArray(bytes);
        } else if (clazz == Character[].class || clazz == char[].class) {
            return toCharArray(bytes);
        } else if (clazz == String[].class) {
            return toStringArray(bytes);
        } else if(clazz == InputStream.class){
            return toInputStream(bytes);
        } else if(clazz == BigInteger.class){
            return toBigInteger(bytes);
        } else if(clazz == BigDecimal.class){
            return toBigDecimal(bytes);
        }
        else {
            throw new RuntimeException("Error: The parameter" + clazz + "is not basic type!");
        }
    }


    public static final byte[] toBytes(byte data) {
        return new byte[]{data};
    }

    public static final byte[] toBytes(byte[] data) {
        return data;
    }

    public static final byte[] toBytes(short data) {
        if (data < Byte.MAX_VALUE) {
            return toBytes((byte) data);
        }
        return new byte[]{
                (byte) ((data >> 8) & 0xff),
                (byte) ((data) & 0xff),
        };
    }

    public static final byte[] toBytes(short[] data) {
        if (data == null) return null;
        // ----------
        byte[] byts = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++)
            System.arraycopy(toBytes(data[i]), 0, byts, i * 2, 2);
        return byts;
    }

    public static final byte[] toBytes(char data) {
        return new byte[]{
                (byte) ((data >> 8) & 0xff),
                (byte) (data & 0xff),
        };
    }

    public static final byte[] toBytes(char[] data) {
        if (data == null) return null;
        byte[] byts = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++)
            System.arraycopy(toBytes(data[i]), 0, byts, i * 2, 2);
        return byts;
    }

    public static final byte[] toBytes(int data) {
        if (data < Short.MAX_VALUE) {
            return toBytes((short) data);
        }
        return new byte[]{
                (byte) ((data >> 24) & 0xff),
                (byte) ((data >> 16) & 0xff),
                (byte) ((data >> 8) & 0xff),
                (byte) ((data) & 0xff),
        };
    }

    public static final byte[] toBytes(int[] data) {
        if (data == null) return null;
        byte[] byts = new byte[data.length * 4];
        for (int i = 0; i < data.length; i++)
            System.arraycopy(toBytes(data[i]), 0, byts, i * 4, 4);
        return byts;
    }

    public static final byte[] toBytes(long data) {//节省redis存储内存
        if (data < Integer.MAX_VALUE) {
            return toBytes((int) data);
        }
        return new byte[]{
                (byte) ((data >> 56) & 0xff),
                (byte) ((data >> 48) & 0xff),
                (byte) ((data >> 40) & 0xff),
                (byte) ((data >> 32) & 0xff),
                (byte) ((data >> 24) & 0xff),
                (byte) ((data >> 16) & 0xff),
                (byte) ((data >> 8) & 0xff),
                (byte) ((data) & 0xff),
        };
    }

    public static final byte[] toBytes(long[] data) {
        if (data == null) return null;
        // ----------
        byte[] byts = new byte[data.length * 8];
        for (int i = 0; i < data.length; i++)
            System.arraycopy(toBytes(data[i]), 0, byts, i * 8, 8);
        return byts;
    }

    public static final byte[] toBytes(float data) {
        return toBytes(Float.floatToRawIntBits(data));
    }

    public static final byte[] toBytes(float[] data) {
        if (data == null) return null;
        // ----------
        byte[] byts = new byte[data.length * 4];
        for (int i = 0; i < data.length; i++)
            System.arraycopy(toBytes(data[i]), 0, byts, i * 4, 4);
        return byts;
    }

    public static final byte[] toBytes(Date date) {
        return toBytes(date.getTime());
    }

    public static final byte[] toBytes(double data) {
        return toBytes(Double.doubleToRawLongBits(data));
    }

    public static final byte[] toBytes(double[] data) {
        if (data == null) return null;
        // ----------
        byte[] byts = new byte[data.length * 8];
        for (int i = 0; i < data.length; i++)
            System.arraycopy(toBytes(data[i]), 0, byts, i * 8, 8);
        return byts;
    }

    public static final byte[] toBytes(boolean data) {
        return new byte[]{(byte) (data ? 0x01 : 0x00)}; // bool -> {1 byte}
    }

    public static final byte[] toBytes(boolean[] data) {
        if (data == null) return null;
        // ----------
        int len = data.length;
        byte[] lena = toBytes(len);
        byte[] byts = new byte[lena.length + (len / 8) + (len % 8 != 0 ? 1 : 0)];
        System.arraycopy(lena, 0, byts, 0, lena.length);

        for (int i = 0, j = lena.length, k = 7; i < data.length; i++) {
            byts[j] |= (data[i] ? 1 : 0) << k--;
            if (k < 0) {
                j++;
                k = 7;
            }
        }

        return byts;
    }

    public static final byte[] toBytes(String data) {
        return (data == null) ? null : data.getBytes();
    }

    public static final byte[] toBytes(Timestamp timestamp) {
        return toBytes(timestamp.getTime());
    }

    public static final byte[] toBytes(InputStream inputStream){
        int size = 4096;
        byte[] bytes = new byte[size];
        int nRead;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            while ((nRead = inputStream.read(bytes, 0, size)) != -1) {
                buffer.write(bytes, 0, nRead);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final byte[] toBytes(BigDecimal decimal){
        return toBytes(decimal.toString());
    }

    public static final byte[] toBytes(BigInteger bigInteger){
        return toBytes(bigInteger.toString());
    }

    public static final byte[] toBytes(String[] data) {

        if (data == null) return null;
        int totalLength = 0;
        int bytesPos = 0;
        byte[] dLen = toBytes(data.length);
        totalLength += dLen.length;
        int[] sLens = new int[data.length];
        totalLength += (sLens.length * 4);
        byte[][] strs = new byte[data.length][];
        for (int i = 0; i < data.length; i++) {
            if (data[i] != null) {
                strs[i] = toBytes(data[i]);
                sLens[i] = strs[i].length;
                totalLength += strs[i].length;
            } else {
                sLens[i] = 0;
                strs[i] = new byte[0];
            }
        }
        byte[] bytes = new byte[totalLength];
        System.arraycopy(dLen, 0, bytes, 0, 4);
        byte[] bsLens = toBytes(sLens);
        System.arraycopy(bsLens, 0, bytes, 4, bsLens.length);
        bytesPos += 4 + bsLens.length;
        for (byte[] sba : strs) {
            System.arraycopy(sba, 0, bytes, bytesPos, sba.length);
            bytesPos += sba.length;
        }
        return bytes;
    }


    public static final byte toByte(byte[] data) {
        return (data == null || data.length == 0) ? 0x0 : data[0];
    }

    public static final byte[] toByteArray(byte[] data) {
        return data;
    }


    public static final short toShort(byte[] data) {
        if (data.length == 1) {
            return (short) data[0];
        }
        return (short) (
                (0xff & data[0]) << 8 | (0xff & data[1]) << 0);
    }

    public static final short[] toShortArray(byte[] data) {
        if (data == null || data.length % 2 != 0) return null;

        short[] shts = new short[data.length / 2];
        for (int i = 0; i < shts.length; i++) {
            shts[i] = toShort(new byte[]{
                    data[(i * 2)],
                    data[(i * 2) + 1]
            });
        }
        return shts;
    }


    public static final char toChar(byte[] data) {
        if (data == null || data.length != 2) return 0x0;
        return (char) (
                (0xff & data[0]) << 8 | (0xff & data[1]) << 0
        );
    }

    public static final char[] toCharArray(byte[] data) {
        if (data == null || data.length % 2 != 0) return null;
        char[] chrs = new char[data.length / 2];
        for (int i = 0; i < chrs.length; i++) {
            chrs[i] = toChar(new byte[]{
                    data[(i * 2)],
                    data[(i * 2) + 1],
            });
        }
        return chrs;
    }

    public static final int toInt(byte[] data) {
        if (data.length == 2 || data.length == 1) {
            return (int) toShort(data);
        }
        if (data.length != 4) {
            return 0;
        }
        return (int) ((0xff & data[0]) << 24 |
                (0xff & data[1]) << 16 |
                (0xff & data[2]) << 8 |
                (0xff & data[3]) << 0
        );
    }

    public static final int[] toIntArray(byte[] data) {
        if (data == null || data.length % 4 != 0) return null;
        int[] ints = new int[data.length / 4];
        for (int i = 0; i < ints.length; i++)
            ints[i] = toInt(new byte[]{
                    data[(i * 4)],
                    data[(i * 4) + 1],
                    data[(i * 4) + 2],
                    data[(i * 4) + 3],
            });
        return ints;
    }

    public static final long toLong(byte[] data) {
        if (data.length == 4 || data.length == 2 || data.length == 1) {
            return (long) toInt(data);
        }
        if (data.length != 8) return 0;
        return (long) (
                // (Below) convert to longs before shift because digits
                //         are lost with ints beyond the 32-bit limit
                (long) (0xff & data[0]) << 56 |
                        (long) (0xff & data[1]) << 48 |
                        (long) (0xff & data[2]) << 40 |
                        (long) (0xff & data[3]) << 32 |
                        (long) (0xff & data[4]) << 24 |
                        (long) (0xff & data[5]) << 16 |
                        (long) (0xff & data[6]) << 8 |
                        (long) (0xff & data[7]) << 0
        );
    }

    public static final long[] toLongArray(byte[] data) {
        if (data == null || data.length % 8 != 0) return null;
        // ----------
        long[] lngs = new long[data.length / 8];
        for (int i = 0; i < lngs.length; i++) {
            lngs[i] = toLong(new byte[]{
                    data[(i * 8)],
                    data[(i * 8) + 1],
                    data[(i * 8) + 2],
                    data[(i * 8) + 3],
                    data[(i * 8) + 4],
                    data[(i * 8) + 5],
                    data[(i * 8) + 6],
                    data[(i * 8) + 7],
            });
        }
        return lngs;
    }

    public static final Timestamp toTimestamp(byte[] data) {
        return new Timestamp(toLong(data));
    }

    public static final Date toDate(byte[] data) {
        return new Date(toLong(data));
    }

    public static final float toFloat(byte[] data) {
        if (data == null || data.length != 4) return 0x0;
        return Float.intBitsToFloat(toInt(data));
    }

    public static final float[] toFloatArray(byte[] data) {
        if (data == null || data.length % 4 != 0) return null;
        float[] flts = new float[data.length / 4];
        for (int i = 0; i < flts.length; i++) {
            flts[i] = toFloat(new byte[]{
                    data[(i * 4)],
                    data[(i * 4) + 1],
                    data[(i * 4) + 2],
                    data[(i * 4) + 3],
            });
        }
        return flts;
    }

    public static final double toDouble(byte[] data) {
        if (data == null || data.length != 8) return 0x0;
        return Double.longBitsToDouble(toLong(data));
    }


    public static final double[] toDoubleArray(byte[] data) {
        if (data == null) return null;
        if (data.length % 8 != 0) return null;
        double[] dbls = new double[data.length / 8];
        for (int i = 0; i < dbls.length; i++) {
            dbls[i] = toDouble(new byte[]{
                    data[(i * 8)],
                    data[(i * 8) + 1],
                    data[(i * 8) + 2],
                    data[(i * 8) + 3],
                    data[(i * 8) + 4],
                    data[(i * 8) + 5],
                    data[(i * 8) + 6],
                    data[(i * 8) + 7],
            });
        }
        return dbls;
    }

    public static final boolean toBoolean(byte[] data) {
        return (data == null || data.length == 0) ? false : data[0] != 0x00;
    }

    public static final boolean[] toBooleanArray(byte[] data) {

        if (data == null || data.length < 4) return null;
        int len = toInt(new byte[]{data[0], data[1], data[2], data[3]});
        boolean[] bools = new boolean[len];
        for (int i = 0, j = 4, k = 7; i < bools.length; i++) {
            bools[i] = ((data[j] >> k--) & 0x01) == 1;
            if (k < 0) {
                j++;
                k = 7;
            }
        }
        return bools;
    }

    public static final byte[][] toBytesArray(String[] data) {
        if (data == null) return null;
        byte[][] bytes = new byte[data.length + 1][];
        int index = 0;
        for (String s : data) {
            bytes[index++] = toBytes(s);
        }
        return bytes;
    }

    public static final String toString(byte[] data) {
        return (data == null) ? null : new String(data);
    }

    public static final String[] toStringArray(byte[] data) {
        if (data == null || data.length < 4) return null;

        byte[] bBuff = new byte[4];
        System.arraycopy(data, 0, bBuff, 0, 4);
        int saLen = toInt(bBuff);
        if (data.length < (4 + (saLen * 4))) return null;
        bBuff = new byte[saLen * 4];
        System.arraycopy(data, 4, bBuff, 0, bBuff.length);
        int[] sLens = toIntArray(bBuff);
        if (sLens == null) return null;
        String[] strs = new String[saLen];
        for (int i = 0, dataPos = 4 + (saLen * 4); i < saLen; i++) {
            if (sLens[i] > 0) {
                if (data.length >= (dataPos + sLens[i])) {
                    bBuff = new byte[sLens[i]];
                    System.arraycopy(data, dataPos, bBuff, 0, sLens[i]);
                    dataPos += sLens[i];
                    strs[i] = toString(bBuff);
                } else return null;
            }
        }

        return strs;
    }

    public static final InputStream toInputStream(byte[] data){
        return new ByteArrayInputStream(data);
    }

    public static final BigDecimal toBigDecimal(byte[] data){
        return new BigDecimal(toString(data));
    }

    public static final BigInteger toBigInteger(byte[] data){
        return new BigInteger(toString(data));
    }

    public static byte[] generateKey(byte[] key, byte[] idByte) {
        byte[] result = new byte[idByte.length + key.length];
        for (int j = 0; j < result.length; j++) {
            if (j < key.length) {
                result[j] = key[j];
            } else {
                result[j] = idByte[j - key.length];
            }
        }
        return result;
    }
}
