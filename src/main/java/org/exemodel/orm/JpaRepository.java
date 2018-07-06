package org.exemodel.orm;

import org.exemodel.session.AbstractSession;
import org.exemodel.session.Session;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author zp [15951818230@163.com]
 */

public abstract class JpaRepository<T extends ExecutableModel> {

    private final Type type;
    private final Session session;


    public JpaRepository() {
        Type superClass = getClass().getGenericSuperclass();
        type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        session = AbstractSession.currentSession();
    }

    public Type getType() {
        return type;
    }

    public Session getSession() {
        return AbstractSession.currentSession();
    }

    public Session session(){
        return session;
    }

    public boolean save(T entity){
        return entity.save();
    }

    public boolean delete(T entity){
        return entity.delete();
    }

    public boolean update(T entity){
        return entity.update();
    }


    public boolean saveBatch(List<T> entities){
        return session.saveBatch(entities);
    }

    public boolean deleteBatch(List<T> entities){
        return session.deleteBatch(entities);
    }

    public boolean updateBatch(List<T> entities){
        return session.updateBatch(entities);
    }




}
