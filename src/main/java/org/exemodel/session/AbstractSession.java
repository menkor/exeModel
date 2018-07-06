package org.exemodel.session;

import org.exemodel.cache.ICache;
import org.exemodel.exceptions.JdbcRuntimeException;
import org.exemodel.orm.ExecutableModel;
import org.exemodel.orm.ModelMeta;
import org.exemodel.session.impl.BeanProcessor;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author zp [15951818230@163.com]
 */
@SuppressWarnings("unchecked")
public abstract class AbstractSession implements Session {
    protected transient boolean isInBatch = false;
    protected transient boolean isInCacheBatch = false;
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
    public void startBatch() {
        this.isInBatch = true;
        ICache cache = getCache();
        if (cache != null) {
            this.isInCacheBatch = true;
            cache.startBatch();
        }
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
    public void startCacheBatch() {
        ICache cache = getCache();
        if (cache == null) {
            throw new JdbcRuntimeException("Please config the cache first");
        }
        this.isInCacheBatch = true;
        cache.startBatch();
    }

    @Override
    public void executeCacheBatch() {
        getCache().executeBatch();
        this.isInCacheBatch = false;
        endCacheBatch();
    }


    protected void endCacheBatch(){
        ICache cache = getCache();
        if(cache!=null){
            this.isInCacheBatch = false;
            cache.endBatch();
        }
    }


    @Override
    public boolean isInCacheBatch() {
        return this.isInCacheBatch;
    }

    @Override
    public boolean updateBatch(List<? extends ExecutableModel> entities) {
        if (entities == null || entities.size() == 0) {
            return true;
        }
        this.isInBatch = true;
        for (ExecutableModel entity : entities) {
            update(entity);
        }
        int[] res = executeBatch();
        if (res == null) {
            return false;
        }
        int len = res.length;
        close();
        ExecutableModel tmp = entities.get(0);
        ModelMeta modelMeta = ModelMeta.getModelMeta(tmp.getClass());
        if (modelMeta.isCacheable()) {
            getCache().batchUpdate(entities);
        }
        return len == entities.size();
    }

    @Override
    public boolean deleteBatch(List<? extends ExecutableModel> entities) {
        if (entities == null || entities.size() == 0) {
            return true;
        }
        this.isInBatch = true;
        for (ExecutableModel entity : entities) {
            delete(entity);
        }
        int len = executeBatch().length;
        close();
        ExecutableModel tmp = entities.get(0);
        ModelMeta modelMeta = ModelMeta.getModelMeta(tmp.getClass());
        if (modelMeta.isCacheable()) {
            getCache().batchDelete(entities);
        }
        return len == entities.size();
    }

}
