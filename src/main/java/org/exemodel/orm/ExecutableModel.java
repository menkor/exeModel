package org.exemodel.orm;
import org.exemodel.cache.ICache;
import org.exemodel.session.AbstractSession;
import org.exemodel.session.Session;
import org.exemodel.util.BinaryUtil;
import org.exemodel.util.Function;
import org.exemodel.util.NumberUtil;
import org.exemodel.util.ParameterBindings;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


public abstract class ExecutableModel implements Serializable{
    private transient boolean valid = true;
    private transient Function function;
    private transient String[] operationFields;
    public static Session getSession() {
        return AbstractSession.currentSession();
    }

    private ICache getCache(){
        return getSession().getCache();
    }

    public boolean save() {
        return save(getSession());
    }
    /**
     * if cacheable,cached to redis
     * @param session current session
     */
    public boolean save(Session session) {
        FieldAccessor idAccessor = ModelMeta.getModelMeta(this.getClass()).getIdAccessor();
        Object tmp = idAccessor.getProperty(this);
        if(NumberUtil.isUndefined(tmp)){
            Object id = generateId();
            if(id!=null){
                idAccessor.setProperty(this,id);
            }
        }
        boolean res = session.save(this);
        ModelMeta meta = ModelMeta.getModelMeta(this.getClass());
        if(meta.isCacheable()){
            getCache().save(this);
        }
        return res;
    }

    public boolean update() {
        return update(getSession());
    }

    public boolean update(Session session) {
        boolean res = session.update(this);
        ModelMeta meta = ModelMeta.getModelMeta(this.getClass());
        if(meta.isCacheable()){
            getCache().update(this);
        }
        return res;
    }

    public boolean delete() {
        boolean res = delete(getSession());
        ModelMeta meta = ModelMeta.getModelMeta(this.getClass());
        if(meta.isCacheable()){
            getCache().delete(this.generateKey());
        }
        return res;
    }

    public boolean delete(Session session) {
        return session.delete(this);
    }


    public static int executeUpdate(String sql, ParameterBindings parameterBindings) {
        return getSession().executeUpdate(sql,parameterBindings);
    }

    public static int executeUpdate(String sql, Object... params){
        return executeUpdate(sql,new ParameterBindings(params));
    }

    public void copyPropertiesTo(Object to){
       getSession().copyProperties(this,to,false,false);
    }

    public void copyPropertiesFrom(Object from){
       getSession().copyProperties(from,this,false,false);
    }

    public void copyPropertiesToAndSkipNull(Object to){
        getSession().copyProperties(this,to,true,true);
    }

    public void copyPropertiesFromAndSkipNull(Object from){
        getSession().copyProperties(from,this,true,true);
    }

    public Map<byte[],byte[]> generateHashByteMap(){ return getSession().generateHashByteMap(this);}

    public byte[] generateKey(){
        ModelMeta meta = ModelMeta.getModelMeta(this.getClass());
        Object id= BinaryUtil.getBytes(meta.getIdAccessor().getProperty(this));
        return  BinaryUtil.generateKey(meta.getKey(),BinaryUtil.getBytes(id));
    }

    public boolean valid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
        if(valid&&function!=null){
            function.apply(this);
        }
    }

    public void onValid(Function func){
        this.function = func;
    }

    public String[] operateFields() {
        return operationFields;
    }

    public <T> T load(Object id){
        return getSession().find(this.getClass(),id);
    }
    

    public void setOperationFields(String[] operationFields) {
        this.operationFields = operationFields;
    }

    public Object generateId(){
        return null;
    }
}
