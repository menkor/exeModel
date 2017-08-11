package org.exemodel.util;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StringUtil {
    public static final String EMPTY = "";
    private final static String str = "BCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz123456789";
    private final static String[] strs =str.split("");
    private final static int strsLength =strs.length;

    public static final int INDEX_NOT_FOUND = -1;

    static {
        strs[0] ="A";
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() < 1;
    }

    public static boolean equalsIgnoreCase(String s1, String s2) {
        return (s1 == null && s2 == null) || (s1 != null && s1.equalsIgnoreCase(s2));
    }

    public static boolean notEmpty(String str) {
        return !isEmpty(str);
    }

    public static String randomString(int n) {
        if (n < 1) {
            n = 1;
        }
        Random random = new Random();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int num = random.nextInt(str.length());
            buf.append(str.charAt(num));
        }
        return buf.toString();
    }

    public static String join(List<?> strs, String sep) {
        StringBuilder builder = new StringBuilder();
        if (strs == null) {
            return null;
        }
        if (sep == null) {
            sep = "";
        }
        for (int i = 0; i < strs.size(); ++i) {
            if (i > 0) {
                builder.append(sep);
            }
            builder.append(strs.get(i));
        }
        return builder.toString();
    }

    public static String joinParams(String sep,String... strs) {
        if(strs==null||strs.length==0){
            return " * ";
        }
        StringBuilder builder = new StringBuilder();
        if (sep == null) {
            sep = "";
        }
        for (int i = 0; i < strs.length; ++i) {
            if (i > 0) {
                builder.append(sep);
            }
            builder.append(StringUtil.underscoreName(strs[i]));
        }
        return builder.toString();
    }

    public  static <E> String join(String sep,List<E> items,MapTo<String,E> mapTo){
        if(items.size()==0){
            return null;
        }
        List<String> strings = new ArrayList<>();
        for(E e:items){
            strings.add(mapTo.apply(e));
        }
        return join(strings,sep);
    }

    /**
     * eg. "HelloWorld" to "hello_world"
     * @param name name to underscore
     * @return underscored name
     */
    public static String underscoreName(String name) {
        if(name ==null) {
            return null;
        }
        int length =  name.length()*3/2;
        char[] rs = new char[length];
        int i=1;
        char tmp;
        rs[0] = Character.toLowerCase(name.charAt(0));
        for(int j=1;j<name.length();j++){
            tmp = name.charAt(j);
            if(tmp>='A'&&tmp<='Z'){
                rs[i] = '_';
                i++;
            }
            rs[i] =  Character.toLowerCase(tmp);
            i++;
        }
        return String.valueOf(rs,0,i);
    }


    public static String[] underscoreNames(String[] names){
        String[] results = new String[names.length];
        for(int i=0,l=names.length;i<l;i++){
            results[i] = underscoreName(names[i]);
        }
        return results;
    }



    /**
     * eg. "HELLO_WORLD" to "HelloWorld"
     * @param name name to camel
     * @return cameled name
     */
    public static String camelName(String name) {
        StringBuilder result = new StringBuilder();
        if (name == null || name.isEmpty()) {
            return "";
        } else if (!name.contains("_")) {
            return name.substring(0, 1).toLowerCase() + name.substring(1);
        }
        String camels[] = name.split("_");
        for (String camel : camels) {
            if (camel.isEmpty()) {
                continue;
            }
            if (result.length() == 0) {
                result.append(camel.toLowerCase());
            } else {
                result.append(camel.substring(0, 1).toUpperCase());
                result.append(camel.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }

    public static ByteArrayOutputStream readFullyInputStreamToBytesStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        final int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        do {
            int readSize = inputStream.read(buffer, 0, bufferSize);
            if (readSize < 1) {
                break;
            }
            byteArrayOutputStream.write(buffer, 0, readSize);
        } while (true);
        return byteArrayOutputStream;
    }

    public static String readFullyInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            byteArrayOutputStream = readFullyInputStreamToBytesStream(inputStream);
            return new String(byteArrayOutputStream.toByteArray(), "UTF-8");
        } finally {
            if (byteArrayOutputStream != null) {
                byteArrayOutputStream.close();
            }
        }
    }

    public static String difference(String str1, String str2) {
        if (str1 == null) {
            return str2;
          }
        if (str2 == null) {
           return str1;
         }
        int at = indexOfDifference(str1, str2);
        if (at == INDEX_NOT_FOUND) {
           return EMPTY;
        }
        return str2.substring(at);
    }

    public static String longToString(long n){
        if(n==0){
            return "A";
        }
        StringBuilder sb = new StringBuilder();
        int a=0;
        while (n>0) {
            a = (int)(n%strsLength);
            n =  n/strsLength;
            sb.append(strs[a]);
        }

        return sb.toString();
    }

    public static String generateId(long number,int length){
        String s = longToString(number);
        int diff = length-s.length();

        if(diff>0){
            StringBuilder sb = new StringBuilder(s);
            for(int i=0;i<diff;i++){
                sb.append('0');
            }
            return sb.toString();
        }else {
            return s;
        }
    }

    public static String escapeSql(String str) {
        return str == null?null: str.replace("\'", "\'\'");
    }

    public static int indexOfDifference(String str1, String str2) {
        if (str1 == str2) {
            return INDEX_NOT_FOUND;
        }
        if (str1 == null || str2 == null) {
            return 0;
        }
        int i;
        for (i = 0; i < str1.length() && i < str2.length(); ++i) {
            if (str1.charAt(i) != str2.charAt(i)) {
                break;
            }
        }
        if (i < str2.length() || i < str1.length()) {
           return i;
        }
        return INDEX_NOT_FOUND;
    }


    public static String generateKey(long a,long b){
        if(a>b){
            return longToString(a)+longToString(b);
        }
        return longToString(b)+longToString(a);
    }
}
