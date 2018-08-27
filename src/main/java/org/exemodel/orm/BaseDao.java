package org.exemodel.orm;

import org.exemodel.orm.ExecutableModel;
import org.exemodel.orm.ModelMeta;
import org.exemodel.orm.Statement;
import org.exemodel.session.AbstractSession;
import org.exemodel.session.Session;
import org.exemodel.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zp [15951818230@163.com]
 */
@SuppressWarnings("unchecked")
public abstract class BaseDao<T extends ExecutableModel> {

    protected Class clazz;
    protected Session session;
    protected ModelMeta modelMeta;

    public BaseDao() {
        Type superClass = getClass().getGenericSuperclass();
        String className = ((ParameterizedType) superClass).getActualTypeArguments()[0]
                .toString()
                .split(" ")[1];
        try {
            clazz = Class.forName(className);
            session = AbstractSession.currentSession();
            modelMeta = ModelMeta.getModelMeta(clazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Session getSession() {
        return AbstractSession.currentSession();
    }

    public Session session() {
        return session;
    }

    public boolean save(T entity) {
        return session.save(entity);
    }

    public boolean delete(T entity) {
        return session.delete(entity);
    }

    public boolean update(T entity) {
        return session.update(entity);
    }

    public boolean saveBatch(List<T> entities) {
        return session.saveBatch(entities);
    }

    public boolean deleteBatch(List<T> entities) {
        return session.deleteBatch(entities);
    }

    public boolean updateBatch(List<T> entities) {
        return session.updateBatch(entities);
    }

    public <E> T findById(E id, String... fields) {
        return (T) statement().id(id).selectOne(fields);
    }

    public <E> T findById(E id, Object partitionId, String... fields) {
        return statement().id(id).partitionId(partitionId).selectOne(fields);
    }


    public <E> Map<E, T> findMapByIds(Collection<E> ids, String... fields) {

        Map<E, T> map = findCacheByIds(ids, fields);
        if (map != null) {
            return map;
        }
        String idName = modelMeta.getIdName();
        List<T> res = statement().in(idName, ids).selectList(fields);
        if(res ==null||res.size()==0){
            return null;
        }
        map = new HashMap<>();

        for(T t:res){
            map.put((E)modelMeta.getIdAccessor().getProperty(t),t);
        }
        return map;
    }


    public <E> List<T> findListByIds(Collection<E> ids, String... fields) {
        Map<E, T> map = findCacheByIds(ids, fields);
        if (map != null) {
            return (List<T>) map.values();
        }
        String idName = modelMeta.getIdName();
        return statement().in(idName, ids).selectList(fields);
    }



    public boolean update(Object id, Object form) {
        return statement().id(id).setByObject(form) > 0;
    }

    public boolean update(Object id, Map<String, Object> map) {
        return statement().id(id).set(map) > 0;
    }


    public boolean deleteById(Object id) {
        return statement().id(id).remove() > 0;
    }


    protected DStatement statement() {
        return new DStatement(clazz);
    }

    protected class DStatement extends Statement<DStatement> {

        public DStatement(Class<?> clazz) {
            super(clazz);
        }

        public DStatement partitionId(Object value) {
            return eq(getModelMeta().getPartitionColumn().columnName, value);
        }
    }

    private <E> Map<E, T> findCacheByIds(Collection<E> ids, String... fields) {
        if (modelMeta.isCacheable()) {
            if (fields.length == 0 && modelMeta.isAllCached()) {
                Object[] idArray = new Object[ids.size()];
                ids.toArray(idArray);
                fields = modelMeta.getCachedFields();
                return session.getCache().batchGet(idArray, clazz, fields);
            } else {
                Object[] idArray = new Object[ids.size()];
                ids.toArray(idArray);
                byte[][] bytes = modelMeta.convertToBytes(StringUtil.underscoreNames(fields));
                if (bytes != null) {
                    return session.getCache().batchGet(idArray, clazz, fields);
                }
            }
        }
        return null;
    }


}
