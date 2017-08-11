package org.exemodel.util;

/**
 * Created by zp on 2016/7/19.
 */
public class Expr {
    private String left;
    private String op;
    private Object right;
    private String sql=null;

    public Expr(){};

    public Expr(String left,String op,Object right){
        this.left =left;
        this.right = right;
        this.op =op;
    }
    public Expr(String sql){
        this.sql =sql;
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

}
