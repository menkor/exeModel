package org.exemodel.session;

import org.exemodel.cache.ICache;
import org.exemodel.orm.ExecutableModel;
import org.exemodel.orm.FieldAccessor;
import org.exemodel.orm.ModelMeta;
import org.exemodel.session.impl.BeanProcessor;
import org.exemodel.util.BinaryUtil;
import org.exemodel.util.MapTo;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("unchecked")
public abstract class AbstractSession implements Session {
    protected final Queue<Object> txStack = new ConcurrentLinkedQueue<Object>();
    private ICache cache;
    public static BeanProcessor beanProcessor = new BeanProcessor();


    @Override
    public int getIndexParamBaseOrdinal() {
        return 1;
    }

    public static void setBeanProcessor(BeanProcessor beanProcessor) {
        AbstractSession.beanProcessor = beanProcessor;
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
        txStack.poll();
        if (getTransactionNestedLevel() > 0) {
            return;
        }
        getTransaction().rollback();
    }



    public int getTransactionNestedLevel() {
        return txStack.size();
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

    public static Connection getConnection(){
        return defaultSessionFactory.createJdbcConnection();
    }

    @Override
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
                if (toColumnMeta.fieldName.equals(fromColumnMeta.fieldName)){
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


    private volatile boolean pmdKnownBroken = false;

    /**
     * @param stmt
     *            PreparedStatement to fill
     * @param params
     *            Query replacement parameters; <code>null</code> is a valid
     *            value to pass in.
     * @throws SQLException
     *             if a database access error occurs
     */
    protected void fillStatement(PreparedStatement stmt, Object... params)
            throws SQLException {

        // check the parameter count, if we can
        ParameterMetaData pmd = null;
        if (!pmdKnownBroken) {
            try {
                pmd = stmt.getParameterMetaData();
                if (pmd == null) { // can be returned by implementations that don't support the method
                    pmdKnownBroken = true;
                } else {
                    int stmtCount = pmd.getParameterCount();
                    int paramsCount = params == null ? 0 : params.length;

                    if (stmtCount != paramsCount) {
                        throw new SQLException("Wrong number of parameters: expected "
                                + stmtCount + ", was given " + paramsCount);
                    }
                }
            } catch (SQLFeatureNotSupportedException ex) {
                pmdKnownBroken = true;
            }
        }

        // nothing to do here
        if (params == null) {
            return;
        }

        for (int i = 0; i < params.length; i++) {
            if (params[i] != null) {
                stmt.setObject(i + 1, params[i]);
            } else {
                // VARCHAR works with many drivers regardless
                // of the actual column type. Oddly, NULL and
                // OTHER don't work with Oracle's drivers.
                int sqlType = Types.VARCHAR;
                if (!pmdKnownBroken) {
                    try {
                        /*
                         * It's not possible for pmdKnownBroken to change from
                         * true to false, (once true, always true) so pmd cannot
                         * be null here.
                         */
                        sqlType = pmd.getParameterType(i + 1);
                    } catch (SQLException e) {
                        pmdKnownBroken = true;
                    }
                }
                stmt.setNull(i + 1, sqlType);
            }
        }
    }




}
