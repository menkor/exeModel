package org.exemodel;

import org.exemodel.component.*;
import org.exemodel.model.Role;
import org.exemodel.session.AbstractSession;
import org.exemodel.session.Session;
import org.exemodel.util.ParameterBindings;
import org.exemodel.model.User;
import org.junit.Test;
import org.springframework.beans.BeanUtils;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zp on 17/2/16.
 */

public class PerformanceTest {
    static {
        new InitResource();
    }
    /**
     * using 1324ms, start at Thu Sep 08 16:55:34 CST 2016, end at Thu Sep 08 16:55:35 CST 2016
     using 1115ms, start at Thu Sep 08 16:55:35 CST 2016, end at Thu Sep 08 16:55:36 CST 2016
     using 1156ms, start at Thu Sep 08 16:55:36 CST 2016, end at Thu Sep 08 16:55:37 CST 2016
     */
//    @Test
//    public void testPerformanceExecute() {
//        User user = new User();
//        user.setAge(33);
//        user.setName("tms");
//        user.save();
//
//        Timer timer = new Timer();
//        for (int i = 0; i < 1000; i++) {
//            CustomStatement.build(User.class).eq("name", "tms").set("name", "xxf", "age", 38);//把tms改成xxf，年龄改为38
//        }
//        timer.end();
//
//        Timer timer1 = new Timer();
//        for (int i = 0; i < 1000; i++) {
//            User.executeUpdate("update user set name = ? ,age=? where name =?", new ParameterBindings("tms", 38, "xxf"));
//        }
//        timer1.end();
//        DataSource dataSource = InitResource.dataSource;
//        Connection connection = null;
//        PreparedStatement preparedStatement = null;
//        Timer timer2 = new Timer();
//        for (int i = 0; i < 1000; i++) {
//
//            try {
//                connection = dataSource.getConnection();
//                preparedStatement = connection.prepareStatement("update user set name = ? ,age=? where name =?");
//                preparedStatement.setObject(1, "xxf");
//                preparedStatement.setObject(2, 38);
//                preparedStatement.setObject(3, "tms");
//                preparedStatement.executeUpdate();
//                preparedStatement.close();
//
//            } catch (SQLException e) {
//                e.printStackTrace();
//            } finally {
//                try {
//                    if (connection != null)
//                        connection.close();
//                } catch (SQLException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        }
//        timer2.end();
//    }

    /**
     * using 385ms, start at Sun Sep 18 15:54:39 CST 2016, end at Sun Sep 18 15:54:39 CST 2016
     using 932ms, start at Sun Sep 18 15:54:39 CST 2016, end at Sun Sep 18 15:54:40 CST 2016
     */
    @Test
    public void  testCopyProperties(){
        UserAddForm userAddForm = new UserAddForm();
        userAddForm.setName("zp");
        userAddForm.setAge(10);

        Timer timer =new Timer();
        for(int i=0;i<1000000;i++){
            User user = new User();
            user.copyPropertiesFrom(userAddForm);
        }
        timer.end();

        Timer timer1=new Timer();
        for(int i=0;i<1000000;i++){
            User user = new User();
            BeanUtils.copyProperties(userAddForm,user);
        }
        timer1.end();
    }


    /**
     using 25137ms, start at Thu Feb 23 10:39:24 CST 2017, end at Thu Feb 23 10:39:49 CST 2017
     using 24400ms, start at Thu Feb 23 10:39:49 CST 2017, end at Thu Feb 23 10:40:13 CST 2017
     using 64ms, start at Thu Feb 23 10:40:13 CST 2017, end at Thu Feb 23 10:40:13 CST 2017
     using 703ms, start at Thu Feb 23 10:40:13 CST 2017, end at Thu Feb 23 10:40:14 CST 2017
     */
    @Test
    public void testCacheBatch(){
        try(final Session session = AbstractSession.currentSession()){//try with material
           final List<Role> roleList = new ArrayList<>();
            for(int i=0;i<100;i++){
                Role role = new Role();
                role.setPermissions("fuck dog");
                role.setTitle("jyz_FuckDog");
                role.setUserId(10+i);
                roleList.add(role);
            }
            session.saveBatch(roleList);

            Timer.compair(10, new Execution() {
                @Override
                public void execute() {
                    session.startBatch();
                    for (Role r : roleList) {
                        r.update();
                    }
                    session.executeBatch();
                }
            }, new Execution() {
                @Override
                public void execute() {
                    for (Role r : roleList) {
                        r.update();
                    }
                }
            }, new Execution() {
                @Override
                public void execute() {
                    session.startCacheBatch();
                    for (Role r : roleList) {

                        Role role = CustomStatement.build(Role.class).findCache(r.getId());
                    }
                    session.executeCacheBatch();
                }
            }, new Execution() {
                @Override
                public void execute() {
                    for (Role r : roleList) {
                        Role role = CustomStatement.build(Role.class).findCache(r.getId());
                    }
                }
            });

        }
    }
}
