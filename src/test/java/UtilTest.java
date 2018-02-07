import model.Role;
import model.User;
import org.exemodel.orm.ModelMeta;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
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
