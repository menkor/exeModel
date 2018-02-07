package org.exemodel.session;


import org.exemodel.cache.ICache;
import org.exemodel.orm.ExecutableModel;
import org.exemodel.transation.Transaction;
import org.exemodel.util.MapTo;
import org.exemodel.util.Pagination;
import org.exemodel.util.ParameterBindings;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A session hold by each Thread, combine rds and cache operation
 * Created by zp on 2016/7/18
 */
public interface Session extends AutoCloseable{

    /**
     *
     * @return the preparedStatement.setObject begin index, common 1
     */
    int getIndexParamBaseOrdinal();


    /**
     * @return transaction
     */
    Transaction getTransaction();

    /**
     * transaction begin
     */
    void begin();

    /**
     * transaction commit
     */
    void commit();

    /**
     * connection isOpen
     * @return
     */
    boolean isOpen();

    /**
     * in transaction
     * @return
     */
    boolean isRunning();

    /**
     * transaction rollback
     */
    void rollback();

    /**
     * close connection, end sql statement batch ,end cache batch..
     */
    void close();


    /**
     * save the entity to the rds (relation database)
     * @param entity
     * @return
     */
    boolean save(ExecutableModel entity);

    /**
     * update the entity to the rds
     * @param entity
     * @return
     */
    boolean update(ExecutableModel entity);

    /**
     * update the entity to the rds
     * @param entity
     * @param fields need update,eg if you just need to update few field,You can use this
     * @return
     */
    boolean update(ExecutableModel entity, String... fields);

    /**
     * begin batch, both sql and cache start cache
     */
    void startBatch();

    /**
     * execute batch,if cache,also need to update cache batch,such as redis pipeline
     * @return batchStatement.executeBatch result
     */
    int[] executeBatch();

    /**
     * update rds and cache by entities
     * @param entities need id ,if shard the database, suggest with partitionId
     */
    boolean updateBatch(List<? extends ExecutableModel> entities);

    /**
     * save entities to  rds,and use the rds generate id to save to cache
     * @param entities if shard the database, need partitionId
     */
    boolean saveBatch(List<? extends ExecutableModel> entities);

    /**
     * delete entities from rds and cache
     * @param entities need id ,if shard the database, suggest with partitionId
     */
    boolean deleteBatch(List<? extends ExecutableModel> entities);

    /**
     * find by id
     * @param cls class of result
     * @param id  @id
     * @param <T>
     * @return T
     */
    <T> T find(Class<? extends T> cls, Object id);


    /**
     * find by id and partitionId
     * @param cls
     * @param id
     * @param partitionId
     * @param <T>
     * @return
     */
    <T> T find(Class<? extends T> cls, Object id, Object partitionId);


    /**
     * delete record from rds
     * @param entity need id
     */
    boolean delete(ExecutableModel entity);


    /**
     * use sql to select one
     * @param cls result class
     * @param queryString sql
     * @param params sql params
     * @param <T>
     * @return T
     */
    <T> T findOneByNativeSql(Class<? extends T> cls, String queryString, Object... params);


    /**
     * use sql to select list
     * @param cls result class
     * @param queryString
     * @param params
     * @param <T>
     * @return
     */
    <T> List<T> findListByNativeSql(Class<? extends T> cls, String queryString, Object... params);

    /**
     * use sql to query list
     * @param cls result class
     * @param queryString
     * @param parameterBindings
     * @param pagination
     * @param <T>
     * @return
     */
    <T> List<T> findListByNativeSql(Class<? extends T> cls, String queryString, ParameterBindings parameterBindings,
        Pagination pagination);

    /**
     *use sql to query one
     * @param cls
     * @param queryString
     * @param parameterBindings
     * @param <T>
     * @return
     */
    <T> T findOneByNativeSql(Class<? extends T> cls, String queryString, ParameterBindings parameterBindings);

    /**
     *use sql to query list
     * @param cls
     * @param queryString
     * @param parameterBindings
     * @param <T>
     * @return
     */
    <T> List<T> findListByNativeSql(Class<? extends T> cls, String queryString, ParameterBindings parameterBindings);

    /**
     * update db
     * @param sql
     * @param parameterBindings
     * @return changed count
     */
    int executeUpdate(String sql, ParameterBindings parameterBindings);

    /**
     * update db
     * @param sql
     * @param params
     * @return
     */
    int executeUpdate(String sql, Object[] params);


    void copyProperties(Object from, Object to, boolean skipNull, boolean skipId);

    HashMap<String, Object> generateHashMapFromEntity(Object entity, boolean skipNull);

    HashMap<byte[], byte[]> generateHashByteMap(Object entity);

    <T> T generateHashMapFromEntity(HashMap<String, Object> hashMap, Object entity);

    <T> T generateEntityFromHashMap(HashMap<byte[], byte[]> hashMap, Object entity);

    ICache getCache();

    boolean isInCacheBatch();

    void startCacheBatch();

    void executeCacheBatch();

    <K,V> Map<K,V> batchGetFromCache(K[] ids, Class<V> clazz, String... fields);

    <K,V,E> Map<E,V> batchGetFromCache(Collection<? extends K> source, MapTo<E, K> mapTo,
        Class<V> clazz, String... fields);

    void batchDeleteCache(List<ExecutableModel> models);

    boolean execute(String sql);
}
