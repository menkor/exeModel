import org.exemodel.orm.Statement;
import org.exemodel.session.AbstractSession;
import org.exemodel.session.Session;
import org.exemodel.exceptions.JdbcRuntimeException;
import org.exemodel.orm.ExecutableModel;
import org.exemodel.util.Expr;
import org.exemodel.util.Pagination;
import org.exemodel.util.ParameterBindings;
import junit.framework.TestCase;
import model.Role;
import model.User;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by zp on 2016/7/20.
 */
public class UserDaoTest extends TestCase {
    static {
        new InitResource();
    }
    public void testFindById() {
//        User user = User.findById()
    }

    public User testSave() {
        User user = new User();
        user.setName("zp");
        user.setAge(18);
        user.save();
        Assert.assertTrue(CustomStatement.build(User.class).findById(user.getId()) != null);
        return user;
    }


    public void testUpdate() {
        User user = CustomStatement.build(User.class).findById(1);
        user.setAge(25);
        user.update();
        User _user = CustomStatement.build(User.class).findById(1);
        Assert.assertTrue(_user.getAge() == 25);
    }


    public void testDelete() {
        User user = new User();
        user.setName("src/test");
        user.setAge(18);
        user.save();
        user.delete();
        Assert.assertTrue(CustomStatement.build(User.class).findById(user.getId()) == null);
    }


    public void testFindList() {
        List<User> list = CustomStatement.build(User.class).findListByNativeSql("select * from user where name=?", "zp");
        Assert.assertTrue(list != null);
    }

    public void testFindOne() {
        testSave();
        User user = CustomStatement.build(User.class).findOneByNativeSql("select id,name as idCard from user where name=? and age =? limit 1 ", "zp",18);
        Assert.assertTrue(user != null);
    }

    public void testSelectOne() {
        testSave();
        User user = CustomStatement.build(User.class).eq("name", "xxf").selectOne("id", "name");
        Assert.assertTrue(user != null);
    }


    public void testSelectList() {
        List<User> users = CustomStatement.build(User.class).eq("name", "zp").asc("age").selectList("id","name");
        Assert.assertTrue(users != null);
        List<Long> ids =(List<Long>) CustomStatement.build(User.class).eq("name", "zp").asc("age").selectList(Long.class,"id");
        Assert.assertTrue(ids != null);

    }

    public void testOrConditions(){
        List<User> users1 = CustomStatement.build(User.class).or(Expr.eq("name","zp"),Expr.eq("name","xxf")).selectList();
        String[] names ={"zp","xxf"};
        List<User> users2 = CustomStatement.build(User.class).in("name",names).selectList();
        Assert.assertTrue(users1.size()==users2.size());

    }

    public void testRemove() {
        User user = new User();
        user.setAge(33);
        user.setName("tms");
        user.save();
        CustomStatement.build(User.class).eq("name", "tms").remove();
        Assert.assertTrue(CustomStatement.build(User.class).findById(user.getId()) == null);


    }

    public void testHashMapFromEntity() {
        User user = new User();
        user.setName("src/test");
        user.setAge(18);
        user.save();
        HashMap<String,Object> hashMap=User.getSession().generateHashMapFromEntity(user,false);
        Assert.assertTrue(hashMap.get("age").equals(18));

        User user1 = new User();
        User.getSession().generateHashMapFromEntity(hashMap, user1);
        Assert.assertTrue(user1.getAge() == 18);

    }


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

    public void testTransaction(){
        Session session = AbstractSession.currentSession();
        session.begin();
        CustomStatement.build(User.class).eq("name", "tms").set("name", "xxf", "age", 38);//把tms改成xxf，年龄改为38
        CustomStatement.build(User.class).eq("name", "xxf").set(" age = age + 1 ", null);
        session.commit();

    }


    public void testExecute() {
        User.executeUpdate("update user set name=? where name='zp'", new ParameterBindings("jzy"));
        Assert.assertTrue(CustomStatement.build(User.class).eq("name", "zp").selectOne() == null);
    }



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

    public void testPagination() {
        Pagination pagination = new Pagination();
        pagination.setPage(1);
        pagination.setSize(20);
        List<User> users = CustomStatement.build(User.class).eq("name", "xxf").asc("age").selectByPagination(pagination);

        Assert.assertFalse(users.size() > pagination.getTotal());

    }

    public void testUpdateWithPartition(){
        User user=testSave();
        Role role = new Role();
        role.setTitle("开发人员");
        try {
            role.save();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof JdbcRuntimeException);
        }

        role.setUserId(user.getId());
        role.save();
        Assert.assertTrue(role.getUserId()==user.getId());
    }

    public void testDeleteWithPartition(){
        User user=testSave();
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


    @Test(threadPoolSize = 10, invocationCount = 11)
    public void testMutilThread() {
    }


    @org.junit.Test
    public void testExecuteBatch(){
        List<ExecutableModel> users = new ArrayList<>();
        for(int i=0;i<10;i++){
            User user = new User();
            user.setName("zp"+i);
            users.add(user);
        }
        try(Session session =User.getSession()) {
            session.saveBatch(users);//批量保存userlist
        }catch (Exception e){
            e.printStackTrace();
        }
        Session session =User.getSession();
        session.startBatch();
        for(int i=0;i<10;i++){
            User.executeUpdate("update user set name=? where name=?", new ParameterBindings("jzy"+i,"zp"+i));//批量修改
        }
        
        session.executeBatch();
    }


    @org.junit.Test
    public void testIsNull(){

    }

    @org.junit.Test
    public void testRedisBatch(){


    }
}
