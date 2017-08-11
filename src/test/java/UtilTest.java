import model.User;
import org.junit.Assert;
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
        int a = 0;
        Class type = short[].class;
        System.out.println(type.getName());
        type = User[].class;
        System.out.println(type.getName());
    }

}
