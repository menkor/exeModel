package org.exemodel;

import org.exemodel.component.*;
import org.exemodel.exceptions.JdbcRuntimeException;
import org.exemodel.model.Role;
import org.exemodel.model.User;
import org.exemodel.orm.ExecutableModel;
import org.exemodel.session.AbstractSession;
import org.exemodel.session.Session;
import org.exemodel.util.BinaryUtil;
import org.exemodel.util.Expr;
import org.exemodel.util.Pagination;
import org.exemodel.util.ParameterBindings;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zp on 18/8/24.
 */
public class StatementTest {
    static {
        new InitResource();
    }



    @Test
    public void testFindList() {
        String sql = "select * from user where name=?";
        List<User> list = CustomStatement.build(User.class).findListByNativeSql(sql, "zp");

        Assert.assertTrue(list != null);
    }

    @Test
    public void testFindOne() {
        saveUser();
        String sql = "select id,name from user where name=? and age =? limit 1 ";
        User user = CustomStatement.build(User.class).findOneByNativeSql(sql, "zp", 18);
        Assert.assertTrue(user != null);

        UserVO userVO = CustomStatement.build(User.class).eq("name","zp").selectOne(UserVO.class,"id"," name as username");
        Assert.assertTrue(userVO!=null);
    }

    @Test
    public void testSelectOne() {
        saveUser();
        User user = CustomStatement.build(User.class).eq("name", "zp").selectOne("id", "name");
        Assert.assertTrue(user != null);
    }


    @Test
    public void testSelectList() {
        List<User> users = CustomStatement.build(User.class).eq("name", "zp")
                .asc("age")
                .selectList("id", "name");

        Assert.assertTrue(users != null);
        List<Long> ids = CustomStatement.build(User.class).eq("name", "zp")
                .asc("age")
                .selectList(Long.class, "id");
        Assert.assertTrue(ids != null);

    }


    @Test
    public void testOrConditions() {
        List<User> users1 = CustomStatement.build(User.class).or(Expr.eq("name", "zp"), Expr.eq("name", "xxf")).selectList();
        String[] names = {"zp", "xxf"};
        List<User> users2 = CustomStatement.build(User.class).in("name", names).selectList();
        Assert.assertTrue(users1.size() == users2.size());

    }

    @Test
    public void testRemove() {
        User user = new User();
        user.setAge(33);
        user.setName("tms");
        user.save();
        CustomStatement.build(User.class).eq("name", "tms").remove();
        Assert.assertTrue(CustomStatement.build(User.class).findById(user.getId()) == null);


    }


    @Test
    public void testSet() {
        User user = new User();
        user.setAge(33);
        user.setName("tms");
        user.save();
        CustomStatement.build(User.class).eq("name", "tms").set("name", "xxf", "age", 38);//把tms改成xxf，年龄改为38
        User _user = CustomStatement.build(User.class).findById(user.getId());
        Assert.assertTrue(_user.getAge() == 38);

        CustomStatement.build(User.class).eq("name", "xxf").set(" age = age + 1 ", null);
        Object __user = CustomStatement.build(User.class).findById(user.getId());
        Assert.assertTrue(((User) __user).getAge() == 39);

    }

    @Test
    public void testTransaction() {
        Session session = AbstractSession.currentSession();
        session.begin();
        CustomStatement.build(User.class).eq("name", "tms").set("name", "xxf", "age", 38);//把tms改成xxf，年龄改为38
        CustomStatement.build(User.class).eq("name", "xxf").set(" age = age + 1 ", null);
        session.commit();

    }


    @Test
    public void testExecute() {
        User.executeUpdate("update user set name=? where name='zp'", new ParameterBindings("jzy"));
        Assert.assertTrue(CustomStatement.build(User.class).eq("name", "zp").selectOne() == null);
    }


    @Test
    public void testCopy() {
        UserAddForm userAddForm = new UserAddForm();
        userAddForm.setName("zp");
        userAddForm.setAge(10);
        User user = new User();
        user.copyPropertiesFrom(userAddForm);
        Assert.assertTrue(user.getName().equals("zp") && user.getAge() == 10);

        UserAddForm test2 = new UserAddForm();
        user.copyPropertiesTo(test2);
        Assert.assertFalse(test2.getName().equals("zp") && test2.getAge() == null);

    }

    @Test
    public void testPagination() {
        Pagination pagination = new Pagination();
        pagination.setPage(1);
        pagination.setSize(20);
        List<User> users = CustomStatement.build(User.class).eq("name", "xxf")
                .asc("age")
                .selectByPagination(pagination);

        Assert.assertFalse(users.size() > pagination.getTotal());

    }

    @Test
    public void testUpdateWithPartition() {
        User user = saveUser();
        Role role = new Role();
        role.setTitle("开发人员");
        try {
            role.save();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof JdbcRuntimeException);
        }

        role.setUserId(user.getId());
        role.save();
        Assert.assertTrue(role.getUserId() == user.getId());
    }

    @Test
    public void testDeleteWithPartition() {
        User user = saveUser();
        Role role = new Role();
        role.setTitle("开发人员");
        role.setUserId(user.getId());
        try {
            role.delete();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof JdbcRuntimeException);
        }

        role.save();

        try {
            CustomStatement.build(Role.class).findById(role.getId());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof JdbcRuntimeException);
            e.printStackTrace();
        }
        Role _role = CustomStatement.build(Role.class).findById(role.getId(), user.getId());
        Assert.assertTrue(_role.getTitle().equals("开发人员"));

        role.delete();

    }


    @Test
    public void testExecuteBatch() {
        List<ExecutableModel> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setName("zp" + i);
            users.add(user);
        }
        try (Session session = User.getSession()) {
            session.saveBatch(users);//批量保存userlist
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (Session session = User.getSession()) {
            session.startBatch();
            for (int i = 0; i < 10; i++) {
                User.executeUpdate("update user set name=? where name=?", new ParameterBindings("jzy" + i, "zp" + i));//批量修改
            }

            session.executeBatch();
        }
    }

    @Test
    public void testUnusualType(){
        String testStr = "ElonMusk";
        BigDecimal money = new BigDecimal("666.66");
        BigInteger no = new BigInteger("1212121212121212121");
        byte[] bytes = BinaryUtil.getBytes(testStr);
        User user = new User();
        user.setName("zp");
        user.setAge(18);
        user.setPwd(bytes);
        user.setMoney(money);
        user.setSerialNo(no);
        user.save();

        User _user = CustomStatement.build(User.class).findById(user.getId());
        Assert.assertTrue(BinaryUtil.toString(_user.getPwd()).equals(testStr));
        Assert.assertTrue(_user.getMoney().equals(money));
        Assert.assertTrue(_user.getSerialNo().equals(no));
    }

    @Test
    public void testInputStream() throws Exception{
        InputStream inputStream = UserDaoTest.class.getResourceAsStream("/test.sql");
        User user = new User();
        user.setName("zp");
        user.setAge(18);
        user.setImage(inputStream);
        user.save();

        User _user = CustomStatement.build(User.class).findById(user.getId());
        InputStream inputStream1 = _user.getImage();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream1,"utf-8"));
        String s;
        while ( (s = bufferedReader.readLine())!=null){
            System.out.println(s);
        }
    }

    @Test
    public void testMutilResultSetOfProcedure() throws SQLException {
        TestVO testVO = AbstractSession.currentSession()
                .callProcedure(TestVO.class," call test_mutil_result_set(?)",new ParameterBindings("zp"));
    }


    private User saveUser() {
        User user = new User();
        user.setName("zp");
        user.setGender(Gender.MAN);
        user.setAge(18);
        new UserDao().insert(user);
        return user;
    }

}
