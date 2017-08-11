package org.exemodel.session;

import org.exemodel.cache.ICache;
import org.exemodel.orm.ExecutableModel;
import org.exemodel.orm.FieldAccessor;
import org.exemodel.orm.ModelMeta;
import org.exemodel.util.BinaryUtil;
import com.google.common.collect.ImmutableMap;
import org.exemodel.util.MapTo;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("unchecked")
public abstract class AbstractSession implements Session {
    protected final Queue<Object> txStack = new ConcurrentLinkedQueue<Object>();
    private ICache cache;

    @Override
    public int getIndexParamBaseOrdinal() {
        return 1;
    }

    @Override
    public void begin() {
        txStack.add(1);
        if (getTransactionNestedLevel() > 1) {
            return;
        }
        getTransaction().begin();
    }

    @Override
    public boolean isRunning() {
        return txStack.size() > 0;
    }

    @Override
    public void commit() {
        if (!isOpen()) {
            begin();
        }
        txStack.poll();
        if (getTransactionNestedLevel() > 0) {
            return;
        }
        getTransaction().commit();
    }

    @Override
    public int[] executeBatch() {
        return new int[0];
    }


    @Override
    public void rollback() {
        if (!isTransactionActive()) {
            return;
        }
        txStack.poll();
        if (getTransactionNestedLevel() > 0) {
            return;
        }
        getTransaction().rollback();
    }



    public int getTransactionNestedLevel() {
        return txStack.size();
    }

    public boolean isTransactionActive() {
        return getTransaction().isActive();
    }

    private static transient SessionFactory defaultSessionFactory = null;

    public static void setDefaultSessionFactory(SessionFactory sessionFactory) {
        defaultSessionFactory = sessionFactory;
    }

    public static void setDefaultSessionFactoryIfEmpty(SessionFactory sessionFactory) {
        if (sessionFactory == null) {
            return;
        }
        if (defaultSessionFactory == null) {
            synchronized (AbstractSession.class) {
                if (defaultSessionFactory == null) {
                    defaultSessionFactory = sessionFactory;
                }
            }
        }
    }

    /**
     * check whether session binded to current thread first
     * and set result to binded session of current thread
     *
     * @return current session of current thread
     */
    public static Session currentSession() {
        return defaultSessionFactory.currentSession();
    }


    public ICache getCache(){
        if(cache==null){
            ICache iCache = defaultSessionFactory.getCache();
            if(iCache==null){
                return null;
            }
            cache = iCache.clone();
        }
        return cache;
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS
            = new ImmutableMap.Builder<Class<?>, Class<?>>()
            .put(boolean.class, Boolean.class)
            .put(byte.class, Byte.class)
            .put(char.class, Character.class)
            .put(double.class, Double.class)
            .put(float.class, Float.class)
            .put(int.class, Integer.class)
            .put(long.class, Long.class)
            .put(short.class, Short.class)
            .put(void.class, Void.class)
            .build();

    private boolean canSetProperties(ModelMeta.ModelColumnMeta fromColumnMeta,ModelMeta.ModelColumnMeta toColumnMeta){
        if(!toColumnMeta.fieldName.equals(fromColumnMeta.fieldName)){
            return false;
        }
        Class<?> from = fromColumnMeta.fieldType;
        Class<?> to = toColumnMeta.fieldType;
        if(from.equals(to)){
            return true;
        }
        if(from.isPrimitive()){
           return  PRIMITIVES_TO_WRAPPERS.get(from).equals(to);
        }
        if(to.isPrimitive()){
            return PRIMITIVES_TO_WRAPPERS.get(to).equals(from);
        }
        return false;
    }

    @Override
    public void copyProperties(Object from, Object to, boolean skipNull,boolean skipId) {
        if(from==null||to==null){
            return;
        }
        ModelMeta fromMeta = ModelMeta.getModelMeta(from.getClass());
        ModelMeta toMeta = ModelMeta.getModelMeta(to.getClass());
        for (ModelMeta.ModelColumnMeta fromColumnMeta : fromMeta.getColumnMetaSet()) {
            for (ModelMeta.ModelColumnMeta toColumnMeta : toMeta.getColumnMetaSet()) {
                if (fromColumnMeta.isId && skipId) {
                    continue;
                }
                if (canSetProperties(fromColumnMeta,toColumnMeta)) {
                    FieldAccessor fromFa = fromColumnMeta.fieldAccessor;
                    Object value = fromFa.getProperty(from);
                    if (skipNull && value == null) {
                        continue;
                    }
                    FieldAccessor toFa = toColumnMeta.fieldAccessor;
                    toFa.setProperty(to, fromFa.getProperty(from));
                }
            }
        }
    }

    @Override
    public HashMap<String, Object> generateHashMapFromEntity(Object entity,boolean skipNull) {
        ModelMeta meta = ModelMeta.getModelMeta(entity.getClass());
        HashMap<String, Object> hashMap = new HashMap<>(meta.getColumnMetaSet().size());
        for (ModelMeta.ModelColumnMeta modelColumnMeta : meta.getColumnMetaSet()) {
            if(modelColumnMeta.isId&&skipNull){
                continue;
            }
            FieldAccessor fieldAccessor = modelColumnMeta.fieldAccessor;
            Object value = fieldAccessor.getProperty(entity);
            if(value==null&&skipNull){
                continue;
            }

            hashMap.put(modelColumnMeta.fieldName, fieldAccessor.getProperty(entity));
        }
        return hashMap;
    }

    @Override
    public HashMap<byte[], byte[]> generateHashByteMap(Object entity) {
        ModelMeta meta = ModelMeta.getModelMeta(entity.getClass());
        HashMap<byte[], byte[]> hashMap = new HashMap<>(meta.getColumnMetaSet().size());
        for (ModelMeta.ModelColumnMeta modelColumnMeta : meta.getColumnMetaSet()) {
            if(modelColumnMeta.isId||modelColumnMeta.cacheOrder==null){
                continue;
            }
            FieldAccessor fieldAccessor = modelColumnMeta.fieldAccessor;
            hashMap.put(modelColumnMeta.cacheOrder, BinaryUtil.getBytes(fieldAccessor.getProperty(entity)));
        }
        return hashMap;
    }

    @Override
    public <T> T generateHashMapFromEntity(HashMap<String, Object> hashMap, Object entity) {
        ModelMeta meta = ModelMeta.getModelMeta(entity.getClass());
        for (ModelMeta.ModelColumnMeta modelColumnMeta : meta.getColumnMetaSet()) {
            FieldAccessor fieldAccessor = modelColumnMeta.fieldAccessor;
            fieldAccessor.setProperty(entity, hashMap.get(modelColumnMeta.fieldName));
        }
        return (T)entity;
    }

    @Override
    public <T> T generateEntityFromHashMap(HashMap<byte[], byte[]> hashMap, Object entity) {
        ModelMeta meta = ModelMeta.getModelMeta(entity.getClass());
        for (ModelMeta.ModelColumnMeta modelColumnMeta : meta.getColumnMetaSet()) {
            FieldAccessor fieldAccessor = modelColumnMeta.fieldAccessor;
            fieldAccessor.setProperty(entity,BinaryUtil.getValue(hashMap.get(modelColumnMeta.cacheOrder),fieldAccessor.getPropertyType()));
        }
        return (T) entity;
    }

    @Override
    public <K, V> Map<K, V> batchGetFromCache(K[] ids, Class<V> clazz, String... fields) {
        return getCache().batchGet(ids,clazz,fields);
    }

    @Override
    public <K, V, E> Map<E, V> batchGetFromCache(Collection<? extends K> source, MapTo<E, K> mapTo, Class<V> clazz, String... fields) {
        return getCache().batchGet(source,mapTo,clazz,fields);
    }

    @Override
    public void batchDeleteCache(List<ExecutableModel> models) {
        getCache().batchDelete(models);
    }


}
