package org.exemodel.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ParameterBindings {
    private List<Object> indexBindings = new ArrayList<Object>();

    public ParameterBindings() {

    }

    public ParameterBindings(Object... params) {
        for(Object param : params) {
            this.addIndexBinding(param);
        }
    }

    public List<Object> getIndexBindings() {
        return indexBindings;
    }

    public ParameterBindings addIndexBinding(Object value) {
        this.indexBindings.add(value);
        return this;
    }

    public ParameterBindings addIndexBinding(Object... values) {
        for(Object v:values){
            this.indexBindings.add(v);
        }
        return this;
    }

    public ParameterBindings addIndexBinding(Object value,int amount) {
        for(int i=0;i<amount;i++){
            this.indexBindings.add(value);
        }
        return this;
    }



    public ParameterBindings extend(ParameterBindings other) {
        ParameterBindings result = addAll(other);
        this.indexBindings = result.indexBindings;
        return this;
    }

    public ParameterBindings addAll(ParameterBindings other) {
        if (other == null) {
            return this;
        }
        ParameterBindings result = new ParameterBindings();
        result.indexBindings.addAll(this.indexBindings);
        result.indexBindings.addAll(other.indexBindings);
        return result;
    }


    public Object[] getIndexParametersArray() {
        Object[] rs=getIndexBindings().toArray();
        return rs==null?new Object[0]:rs;
    }

    public void clear(){
        indexBindings.clear();
    }

    public void appendToStatement(PreparedStatement ps) throws SQLException{
        int i= 1;//
        for(Object o:indexBindings){
            ps.setObject(i,o);
            i++;
        }
    }

    @Override
    public ParameterBindings clone(){
        ParameterBindings clone = new ParameterBindings();
        for(Object p:indexBindings){
            clone.addIndexBinding(p);
        }
        return clone;
    }

    public String toString(){
        return  StringUtil.join(indexBindings,",");
    }
}
