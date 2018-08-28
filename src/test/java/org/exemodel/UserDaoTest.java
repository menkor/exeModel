package org.exemodel;

import org.exemodel.component.*;
import org.exemodel.model.User;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.util.List;


/**
 * Created by zp on 2016/7/20.
 */
public class UserDaoTest {
    static {
        new InitResource();
    }
    UserDao userDao = new UserDao();


    @Test
    public void testSave(){
        User user = new User();
        user.setName("zp");
        user.setGender(Gender.MAN);
        user.setAge(18);

        userDao.insert(user);

        User find = userDao.findById(user.getId());
        Assert.assertTrue(user.getName().equals(userDao.findById(user.getId()).getName()));
    }

    @Test
    public void testUpdate() {
        User user = saveUser();
        user.setAge(25);
        user.setName("HanMeiMei");
        user.setGender(Gender.FEMALE);
        PublicInfoDTO publicInfoDTO = new PublicInfoDTO();
        publicInfoDTO.setAddress(true);
        user.setPublicInfo(publicInfoDTO);

        userDao.update(user);

        User fromDb = userDao.findById(user.getId());
        Assert.assertTrue(fromDb.getAge() == 25);
        Assert.assertTrue(fromDb.getPublicInfo().isAddress());
        Assert.assertFalse(fromDb.getPublicInfo().isGender());
    }


    @Test
    public void testDelete() {
        User user = new User();
        user.setName("src/test");
        user.setAge(18);

        userDao.insert(user);
        userDao.delete(user);
        User _user = userDao.findById(user.getId());
        Assert.assertTrue(_user == null);
    }

    @Test
    public void testSelect(){
        saveUser();
        String name = "zp";
        List<User> users = userDao.findUsers(name,"age");
        Assert.assertTrue(users!=null);
        Assert.assertTrue(users.get(0).getGender()==null);
        Assert.assertTrue(users.get(0).getAge()!=0);
    }


    private User saveUser() {
        User user = new User();
        user.setName("zp");
        user.setGender(Gender.MAN);
        user.setAge(18);
        userDao.insert(user);
        return user;
    }


}
