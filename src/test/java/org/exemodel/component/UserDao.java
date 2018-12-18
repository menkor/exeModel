package org.exemodel.component;

import org.exemodel.model.User;
import org.exemodel.orm.ExecutableModel;
import org.exemodel.orm.BaseDao;

import java.util.List;

/**
 * Created by zp on 18/8/14.
 */
public class UserDao extends BaseDao<User> {

    public List<User> findUsers(String name,String... columns){
        return  statement().eq("name",name).selectList(columns);
    }

}
