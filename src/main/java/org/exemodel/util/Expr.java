package org.exemodel.util;

import java.util.List;

/**
 * Created by zp on 2016/7/19.
 */
public class Expr {
    private String left;
    private String op;
    private Object right;
    private ParameterBindings parameterBindings;
    private String sql=null;

    public Expr(){};

    public Expr(String left,String op,Object right){
        this.left =left;
        this.right = right;
        this.op =op;
        this.parameterBindings = new ParameterBindings(right);
    }
    public Expr(String sql,ParameterBindings parameterBindings){
        this.sql =sql;
        this.parameterBindings = parameterBindings;
    }

    public static Expr eq(String left,Object right){
        return new Expr(left,"=",right);
    }

    public static Expr gt(String left,Object right){
        return new Expr(left,">",right);
    }

    public static Expr lt(String left,Object right){
        return new Expr(left,"<",right);
    }

    public static Expr ne(String left,Object right){
        return new Expr(left,"<>",right);
    }

    public static Expr le(String left,Object right){
        return new Expr(left,"<=",right);
    }

    public static Expr ge(String left,Object right){
        return new Expr(left,">=",right);
    }

    public static Expr in(String left,List values){
        ParameterBindings parameterBindings = new ParameterBindings();
        return new Expr(inSqlGenerator(" IN ",left,values.toArray(),parameterBindings),parameterBindings);
    }

    public static Expr notIn(String left,List values){
        ParameterBindings parameterBindings = new ParameterBindings();
        return new Expr(inSqlGenerator(" NOT IN ",left,values.toArray(),parameterBindings),parameterBindings);
    }

    public static String inSqlGenerator(String op,String column,Object[] values, ParameterBindings parameterBindings){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" ");
        stringBuilder.append(StringUtil.underscoreName(column));
        stringBuilder.append(" ");
        stringBuilder.append(op);
        stringBuilder.append(" ( ");
        boolean first = true;
        for(Object value:values){
            if(first){
                stringBuilder.append("?");
                first = false;
            }else {
                stringBuilder.append(",?");
            }
            parameterBindings.addIndexBinding(value);
        }
        stringBuilder.append(") ");
        return stringBuilder.toString();
    }

    public String getLeft() {
        return left;
    }

    public void setLeft(String left) {
        this.left = left;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public Object getRight() {
        return right;
    }

    public void setRight(Object right) {
        this.right = right;
    }

    public String getSql(){
        if(sql==null){
            sql = StringUtil.underscoreName(left)+op+"? ";
        }
        return sql;
    }

    public ParameterBindings getParameterBindings() {
        return parameterBindings;
    }

}
