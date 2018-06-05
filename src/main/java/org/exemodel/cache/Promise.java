package org.exemodel.cache;

import org.exemodel.session.AbstractSession;
import org.exemodel.orm.ExecutableModel;
import org.exemodel.orm.ModelMeta;

/**
 * Created by zp on 17/2/20.
 */
public abstract class Promise {
    private ExecutableModel result;
    private ModelMeta modelMeta;
    private String[] fields;

    public Promise(ExecutableModel result) {
        this.result = result;
    }

    public abstract Object onFail();

    public ExecutableModel getResult() {
        return result;
    }

    public ModelMeta getModelMeta() {
        return modelMeta;
    }

    public void setModelMeta(ModelMeta modelMeta) {
        this.modelMeta = modelMeta;
    }

    public String[] getFields() {
        return fields;
    }

    public void setFields(String[] fields) {
        this.fields = fields;
    }

    public void setResult(Object o){
        if(o==null){
            result.setValid(false);
        }else{
            result.copyPropertiesFrom(o);
            result.setValid(true);
        }
    }
}
