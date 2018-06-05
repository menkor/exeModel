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
