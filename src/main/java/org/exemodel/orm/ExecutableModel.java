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
       copyProperties(this,to,false,false);
    }

    public void copyPropertiesFrom(Object from){
       copyProperties(from,this,false,false);
    }

    public void copyPropertiesToAndSkipNull(Object to){
        copyProperties(this,to,true,true);
    }

    public void copyPropertiesFromAndSkipNull(Object from){
        copyProperties(from,this,true,true);
    }

    public Map<byte[],byte[]> generateHashByteMap(){
        ModelMeta meta = ModelMeta.getModelMeta(this.getClass());
        HashMap<byte[], byte[]> hashMap = new HashMap<>(meta.getColumnMetaSet().size());
        for (ModelMeta.ModelColumnMeta modelColumnMeta : meta.getColumnMetaSet()) {
            if(modelColumnMeta.isId||modelColumnMeta.cacheOrder==null){
                continue;
            }
            FieldAccessor fieldAccessor = modelColumnMeta.fieldAccessor;
            hashMap.put(modelColumnMeta.cacheOrder, BinaryUtil.getBytes(fieldAccessor.getProperty(this)));
        }
        return hashMap;}

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

    public void load(Object id){
        ExecutableModel model = getSession().find(this.getClass(),id);
        this.copyPropertiesFrom(model);
    }
    

    public void setOperationFields(String[] operationFields) {
        this.operationFields = operationFields;
    }

    public Object generateId(){
        return null;
    }

    private  void copyProperties(Object from, Object to, boolean skipNull,boolean skipId) {
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
}
