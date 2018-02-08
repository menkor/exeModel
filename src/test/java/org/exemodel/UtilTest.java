package org.exemodel;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zp on 17/1/4.
 */
public class UtilTest {

    private static Map<String,String> map = new HashMap<>();

    @Test
    public void testName() throws Exception{
        int i =1;
        Object j =i;
        System.out.println(Integer.class.isInstance(j));

    }

}
