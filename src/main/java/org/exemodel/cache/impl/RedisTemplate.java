package org.exemodel.cache.impl;

import org.apache.commons.collections.map.HashedMap;
import org.exemodel.cache.ICache;
import org.exemodel.cache.Promise;
import org.exemodel.exceptions.JedisRuntimeException;
import org.exemodel.orm.ExecutableModel;
import org.exemodel.orm.FieldAccessor;
import org.exemodel.orm.ModelMeta;
import org.exemodel.session.AbstractSession;
import org.exemodel.util.BinaryUtil;
import org.exemodel.util.MapTo;
import org.exemodel.util.ParameterBindings;
import org.exemodel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Redis 工具类
 *
 * @author zp
 */
//TODO jedis用超类JedisCommands 来控制,curtPipeline 用超类 PipelineBase, 通过引入不同的实现来操作,支持ShardedJedis
@SuppressWarnings(value = "unchecked")
public class RedisTemplate implements ICache {

    public final static String OK = "OK";
    protected static ReentrantLock lockJedis = new ReentrantLock();
    protected static Logger logger = LoggerFactory.getLogger(RedisTemplate.class);
    private static JedisPool jedisPool = null;
    private String host = "localhost";
    private int port = 6378;
    private int timeout = 2000;
    private int database = 0;
    private boolean ssl = false;
    private String password = null;
    private final static byte[] updateLua = BinaryUtil.getBytes("if redis.call('EXISTS',KEYS[1])==1 then return redis.call('hmset',KEYS[1],unpack(ARGV)) else return 'OK' end");
    private static byte[] updateLuaSha;
    private Pipeline curtPipeline;
    private Jedis curJedis;
    private List<Promise> promises = new ArrayList<>();
    /**
     * 初始化Redis连接池
     */

    public RedisTemplate(JedisPoolConfig jedisPoolConfig, String host, int port, int timeout, String password, int database, boolean ssl) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.ssl = ssl;
        this.database = database;
        if (StringUtil.isEmpty(password)) {
            password = null;
        }
        this.password = password;
        jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, password,database,ssl);
        Jedis jedis = getJedis();
        updateLuaSha = jedis.scriptLoad(updateLua);
        jedis.close();
    }


    public RedisTemplate(String host, int port, int timeout, int database, boolean ssl){
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.ssl = ssl;
        this.database = database;
    }


    @Override
    public ICache clone() {
        return new RedisTemplate(this.host,this.port,this.timeout,this.database,this.ssl);
    }

    public  Jedis getJedis() {
        assert !lockJedis.isHeldByCurrentThread();
        lockJedis.lock();

        if (jedisPool == null) {
            throw new JedisRuntimeException("You should init the pool");
        }
        try {
            return jedisPool.getResource();
        } catch (Exception e) {
            throw new JedisRuntimeException("Get jedis error : " + e);
        } finally {
            lockJedis.unlock();
        }
    }

    @Override
    public boolean save(ExecutableModel entity) {
        Map<byte[],byte[]> map =entity.generateHashByteMap();
        if(curtPipeline!=null){
            curtPipeline.hmset(entity.generateKey(), map);
            promises.add(null);
            return true;
        }
        Jedis jedis = getJedis();
        String result = jedis.hmset(entity.generateKey(), map);
        jedis.close();
        return OK.equals(result);
    }

    @Override
    public boolean batchSave(List<? extends ExecutableModel> entities) {
        try (Jedis jedis = getJedis()){
            Pipeline pipeline = jedis.pipelined();
            for (ExecutableModel entity : entities) {
                pipeline.hmset(entity.generateKey(), entity.generateHashByteMap());
            }
            pipeline.sync();
        }
        return true;
    }


    @Override
    public <K,V> Map<K,V> batchGet(K [] ids, Class<V> clazz, String... fields) {
        Jedis jedis = getJedis();
        ModelMeta modelMeta = ModelMeta.getModelMeta(clazz);
        FieldAccessor idAccessor = modelMeta.getIdAccessor();
        Pipeline pipeline = jedis.pipelined();
        if(ids.length==0){
            return null;
        }
        Map<K,V> result = new HashedMap(ids.length);

        try {
            if (fields == null || fields.length == 0) {
                fields = modelMeta.getCachedFields();
            }
            byte[][] byteFields = modelMeta.convertToBytes(StringUtil.underscoreNames(fields));
            if (byteFields == null) {
                return null;
            }
            List<Response<List<byte[]>>> list = new ArrayList<>(ids.length);
            Response<List<byte[]>> response;
            for (int i = 0, length = ids.length; i < length; i++) {
                byte[] key = BinaryUtil.generateKey(modelMeta.getKey(), BinaryUtil.getBytes(ids[i]));
                response = pipeline.hmget(key, byteFields);
                list.add(response);
            }
            pipeline.sync();

            int i = 0;
            String sql = null;
            for (Response<List<byte[]>> rsp : list) {
                V entity = (V)generateObjectByList(modelMeta, rsp.get(), fields);
                K id = ids[i++];
                if (entity == null) {
                    if(sql==null){
                        StringBuilder sb = new StringBuilder(" SELECT ");
                        sb.append(StringUtil.joinParams(",",modelMeta.getCachedFields()));
                        sb.append(" FROM ");
                        sb.append(modelMeta.getTableName());
                        sb.append(" WHERE ");
                        sb.append(modelMeta.getIdColumnMeta().columnName);
                        sb.append(" = ? limit 1 ");
                        sql = sb.toString();
                    }
                    entity = AbstractSession.currentSession().findOneByNativeSql(clazz,sql,new ParameterBindings(id));
                    if(entity!=null){
                        save((ExecutableModel) entity);
                    }
                }
                result.put(id, entity);
            }
            return result;
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage());
            return null;
        } catch (InstantiationException e) {
            logger.error(e.getMessage());
            return null;
        }finally {
            jedis.close();
        }
    }

    @Override
    public <K, V,E> Map<E, V> batchGet(Collection<? extends K> source, MapTo<E,K> mapTo, Class<V> clazz, String... fields) {
        if(source==null||source.size()==0){
            return null;
        }
        HashSet<E> hashSet = new HashSet<>();
        for (K k:source){
            if(k!=null){
                hashSet.add(mapTo.apply(k));
            }
        }
        return batchGet((E[]) hashSet.toArray(),clazz,fields);
    }

    @Override
    public boolean delete(byte[] key) {
        if(curtPipeline!=null){
            curtPipeline.del(key);
            promises.add(null);
            return true;
        }
        Jedis jedis = getJedis();
        Long result = jedis.del(key);
        jedis.close();
        return result>0;
    }

    @Override
    public boolean batchDelete(List<? extends ExecutableModel> entities) {
        Jedis jedis = getJedis();
        Pipeline pipeline = jedis.pipelined();
        for (ExecutableModel model : entities) {
            pipeline.del(model.generateKey());
        }
        pipeline.sync();
        return true;

    }

    /**
     * @param modelMeta
     * @param list
     * @param fields    value map list
     * @return
     */
    private Object generateObjectByList(ModelMeta modelMeta, List<byte[]> list, String... fields) throws IllegalAccessException, InstantiationException {
        boolean valid = false;
        for(byte[] tmp:list){
            if(tmp!=null){
                valid = true;
            }
        }
        if(!valid){
            return null;
        }
        Object result = modelMeta.getModelCls().newInstance();
        int i = 0;
        for (String field : fields) {
            FieldAccessor fieldAccessor = FieldAccessor.getFieldAccessor(modelMeta.getModelCls(), StringUtil.camelName(field));
            fieldAccessor.setProperty(result, BinaryUtil.getValue(list.get(i++), fieldAccessor.getPropertyType()));
        }
        return result;
    }

    private Object generateObjectByMap(ModelMeta modelMeta, Map<byte[], byte[]> map) throws IllegalAccessException, InstantiationException {
        Object entity = modelMeta.getModelCls().newInstance();
        for (ModelMeta.ModelColumnMeta modelColumnMeta : modelMeta.getColumnMetaSet()) {
            if (modelColumnMeta.cacheOrder != null) {
                FieldAccessor fieldAccessor = modelColumnMeta.fieldAccessor;
                Object value = BinaryUtil.getValue(map.get(modelColumnMeta.cacheOrder), fieldAccessor.getPropertyType());
                fieldAccessor.setProperty(entity, value);
            }
        }
        return entity;
    }


    public <T> T get(Object id, Class<?> clazz,String... fields) {
        ModelMeta modelMeta = ModelMeta.getModelMeta(clazz);
        FieldAccessor idAccessor = modelMeta.getIdAccessor();
        byte[] key = BinaryUtil.generateKey(modelMeta.getKey(), BinaryUtil.getBytes(id));
        Jedis jedis = getJedis();
        try {
            Object result;
            if (fields == null || fields.length == 0) {
                Map<byte[], byte[]> map = jedis.hgetAll(key);
                if(map.size()==0){
                    return null;
                }
                result = generateObjectByMap(modelMeta, map);
            } else {
                byte[][] bytes = modelMeta.convertToBytes(StringUtil.underscoreNames(fields));
                if (bytes == null) {
                    return null;
                }
                List<byte[]> list = jedis.hmget(key, bytes);
                result = generateObjectByList(modelMeta, list, fields);
            }
            idAccessor.setProperty(result, id);
            return (T) result;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new JedisRuntimeException(e.getMessage());
        }finally {
            jedis.close();
        }
    }

    @Override
    public <T> T get(Object id, Class<?> clazz, Promise promise, byte[][] bytes, String[] fields) {
        ModelMeta modelMeta = ModelMeta.getModelMeta(clazz);
        FieldAccessor idAccessor = modelMeta.getIdAccessor();
        byte[] key = BinaryUtil.generateKey(modelMeta.getKey(), BinaryUtil.getBytes(id));
        if(curtPipeline!=null){
            if(promise==null){
                throw new JedisRuntimeException("Batch get need promise");
            }
            ExecutableModel result =  promise.getResult();
            idAccessor.setProperty(result, id);
            promise.setModelMeta(modelMeta);
            promise.setFields(fields);
            promises.add(promise);
            curtPipeline.hmget(key,bytes);
            return (T) result;
        }

        Jedis jedis = getJedis();
        try {
            List<byte[]> list = jedis.hmget(key, bytes);
            Object result = generateObjectByList(modelMeta, list, fields);
            if(result!=null){
                idAccessor.setProperty(result, id);
            }
            return (T) result;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new JedisRuntimeException(e.getMessage());
        }finally {
            jedis.close();
        }
    }

    private static byte[][] generateUpdateLuaAGRV(ExecutableModel entity) {
        ModelMeta modelMeta = ModelMeta.getModelMeta(entity.getClass());
        List<String> cacheIndex = modelMeta.getCacheColumnList();
        Object id = modelMeta.getIdAccessor().getProperty(entity);
        if (id == null) {
            throw new RuntimeException("id is null");
        }
        byte[] redisKey = BinaryUtil.generateKey(modelMeta.getKey(), BinaryUtil.getBytes(id));
        byte[][] bytes = new byte[cacheIndex.size() * 2 + 1][];
        bytes[0] = redisKey;
        int i=1;
        int order =0;
        for(String index:cacheIndex){
            bytes[i++] = BinaryUtil.getBytes(order++);
            String field = StringUtil.camelName(index);
            Object value = FieldAccessor.getFieldAccessor(entity.getClass(), field).getProperty(entity);
            bytes[i++] = BinaryUtil.getBytes(value);
        }
        return bytes;
    }

    @Override
    public boolean update(ExecutableModel entity) {
        return update(generateUpdateLuaAGRV(entity));
    }

    @Override
    public boolean update(byte[][] argv) {
        if(curtPipeline!=null){
            curtPipeline.evalsha(updateLuaSha, 1, argv);
            promises.add(null);
            return true;
        }
        Jedis jedis = getJedis();
        Object result = jedis.evalsha(updateLuaSha, 1, argv) ;
        jedis.close();
        return  result!=null;
    }


    @Override
    public boolean batchUpdate(List<? extends ExecutableModel> models) {
        Jedis jedis = getJedis();
        Pipeline pipeline = jedis.pipelined();
        for (ExecutableModel model : models) {
            pipeline.evalsha(updateLuaSha, 1, generateUpdateLuaAGRV(model));
        }
        pipeline.sync();
        jedis.close();
        return true;
    }

    public byte[][] generateZipMap(Object entity) {
        ModelMeta meta = ModelMeta.getModelMeta(entity.getClass());
        byte[][] result = new byte[(meta.getColumnMetaSet().size()-1)*2+1][];//因为id不需要存入zipmap
        byte[] key=  meta.getKey();
        int i= 1;

        for (ModelMeta.ModelColumnMeta modelColumnMeta : meta.getColumnMetaSet()) {
            if(modelColumnMeta.isId){
                Object id= BinaryUtil.getBytes(meta.getIdAccessor().getProperty(entity));
                byte[] idByte = BinaryUtil.getBytes(id);
                result[0]= new byte[idByte.length+key.length];
                for(int j=0;j<result[0].length;j++){
                    if(j<key.length){
                        result[0][j]=key[j];
                    }else {
                        result[0][j] = idByte[j-key.length];
                    }
                }
                continue;
            }
            result[i++] = modelColumnMeta.cacheOrder;
            FieldAccessor fieldAccessor = modelColumnMeta.fieldAccessor;
            Object value = fieldAccessor.getProperty(entity);
            result[i++]= BinaryUtil.getBytes(value);
        }
        return result;
    }


    @Override
    public  void startBatch() {
        this.curJedis = getJedis();
        this.curtPipeline = this.curJedis.pipelined();
    }

    @Override
    public void endBatch() {
        this.curtPipeline =null;
        if(this.curJedis!=null){
            this.curJedis.close();
            this.curJedis = null;
        }
        promises.clear();
    }

    @Override
    public boolean executeBatch() {
        if(this.curtPipeline==null){
            return false;
        }
        List<Object> responses = this.curtPipeline.syncAndReturnAll();
        int i=0;
        for(Object rt:responses){
            Promise promise = promises.get(i++);
            if(promise!=null){
                List<byte[]> bytes = (List<byte[]>) rt;
                try {
                    Object var1 = generateObjectByList(promise.getModelMeta(),bytes,promise.getFields());
                    if(var1==null){
                        ExecutableModel getBySql = (ExecutableModel)promise.onFail();
                        if(getBySql!=null){
                           save(getBySql);
                        }
                        promise.setResult(getBySql);
                    }else{
                        promise.setResult(var1);
                    }
                } catch (Exception e) {
                    promise.setResult(promise.onFail());
                }
            }
        }
        endBatch();
        return true;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
