package org.exemodel.cache;

import org.exemodel.orm.ExecutableModel;
import org.exemodel.util.MapTo;
import redis.clients.jedis.Jedis;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by zp on 17/1/12.
 */
public interface ICache {
    /**
     * save the entity to cache,need id
     * @param entity
     * @return
     */
    boolean save(ExecutableModel entity);

    /**
     * delete entity from cache,the key generate like ExecutableModel.generateKey
     * @param key
     * @return
     */
    boolean delete(byte[] key);

    /**
     * update entity to cache, only effect the cached field
     * @param entity
     * @return
     */
    boolean update(ExecutableModel entity);

    /**
     * update argv to cache,the argv is {idKey,fieldKey,fieldValue,fieldKey,fieldValue....}
     * @param argv
     * @return
     */
    boolean update(byte[][] argv);

    /**
     *
     * @param id
     * @param clazz
     * @param fields
     * @param <T>
     * @return
     */
    <T> T get(Object id, Class<?> clazz, String... fields);
    /**
     *
     * @param id
     * @param clazz
     * @param promise
     * @param fields
     * @param <T>
     * @return
     */
    <T> T get(Object id, Class<?> clazz, Promise promise, byte[][] fieldBytes, String[] fields);

    /**
     * batch get entities from cache
     * @param ids entity id list
     * @param clazz entity class
     * @param fields return object's fields
     * @return
     */
    <K,V> Map<K,V> batchGet(K[] ids, Class<V> clazz, String... fields);

    <K,V,E> Map<E,V> batchGet(Collection<? extends K> source, MapTo<E, K> mapTo, Class<V> clazz,
        String... fields);

    /**
     * batch save entities to cache
     * @param entities mast with id
     * @return
     */
    boolean batchSave(List<? extends ExecutableModel> entities);

    /**
     * batch delete entities to cache
     * @param entities mast with id
     * @return
     */
    boolean batchDelete(List<? extends ExecutableModel> entities);

    /**
     * batch update entities to cache
     * @param entities mast with id
     * @return
     */
    boolean batchUpdate(List<? extends ExecutableModel> entities);

    /**
     * is develop stage, this stage will check the valid of arguments,
     * eg. if you update a cacheable entity  fields not with id, this method will throw exception
     * @return
     */

    void startBatch();

    void endBatch();

    boolean executeBatch();

    /**
     * each session will hold a private ICache
     * @return
     */
    ICache clone();

    Jedis getJedis();

}
