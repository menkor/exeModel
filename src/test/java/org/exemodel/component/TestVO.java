package org.exemodel.component;

import org.exemodel.model.User;
import java.util.List;

/**
 * Created by xiaofengxu on 18/2/12.
 */
public class TestVO {
    private User user;
    private String name;
    private List<RoleVO> roleVOs;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RoleVO> getRoleVOs() {
        return roleVOs;
    }

    public void setRoleVOs(List<RoleVO> roleVOs) {
        this.roleVOs = roleVOs;
    }
}
