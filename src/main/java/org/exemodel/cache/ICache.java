package org.exemodel.cache;

import org.exemodel.orm.ExecutableModel;
import org.exemodel.util.MapTo;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author zp [15951818230@163.com]
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
     * get a entity from cache
     * @param id entity id
     * @param clazz entity class
     * @param fields fields need to fill
     * @param <T>
     * @return entity fill with desired fields, other fields value null
     */
    <T> T get(Object id, Class<?> clazz, String... fields);

    /**
     * get a entity from cache, use for batch get
     * @param id  entity id
     * @param clazz entity class
     * @param promise to fill the entity when endBatch
     * @param fieldBytes
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



    <S,K,V> Map<K,V> batchGet(Collection<? extends S> source, MapTo<K, S> getKey, Class<V> clazz,
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


    void startBatch();

    void endBatch();

    boolean executeBatch();
    /**
     * each session will hold a private ICache
     * @return ICache
     */
    ICache clone();


}
