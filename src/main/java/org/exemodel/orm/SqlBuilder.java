package org.exemodel.orm;

import org.exemodel.util.Expr;
import org.exemodel.util.ParameterBindings;
import org.exemodel.util.StringUtil;

import java.util.List;

/**
 * Created by zp on 17/6/19.
 */
@SuppressWarnings("unchecked")
public abstract class SqlBuilder<T> {
    protected ParameterBindings parameterBindings = new ParameterBindings();
    protected StringBuilder where = new StringBuilder(" WHERE (1=1)");

    public T and(String column, String op, Object value){
        where.append(" AND ");
        where.append(StringUtil.underscoreName(column));
        where.append(op);
        where.append("?");
        parameterBindings.addIndexBinding(value);
        return (T) this;
    }

    /**
     * equal
     */
    public T eq(String column, Object value){
        return (T)and(column,"=",value);
    }

    /**
     * not equal
     */
    public T ne(String column, Object value){
        return and(column,"<>",value);
    }

    /**
     *like
     */
    public T like(String column, Object value){return and(column," LIKE ",value);}

    /**
     * state equal
     */
    public T state(Object value){
        return and("STATE","=",value);
    }

    /**
     * greater than
     */
    public T gt(String column, Object value){
        return and(column,">",value);
    }

    /**
     * less than
     */
    public T lt(String column, Object value){
        return and(column,"<",value);
    }


    /**
     * less than or equal
     */
    public T le(String column, Object value){
        return and(column,"<=",value);
    }

    /**
     * greater than or equal
     */
    public T ge(String column, Object value){
        return and(column,">=",value);
    }

    public T isNull(String column){
        where.append(" AND ");
        where.append(column);
        where.append(" IS NULL ");
        return (T)this;
    }

    public T isNotNull(String column){
        where.append(" AND ");
        where.append(column);
        where.append(" IS NOT NULL ");
        return (T)this;
    }


    private T inSqlGenerate(String column, Object[] values,String op){//
        where.append(" AND ");
        where.append(column);
        where.append(op);
        where.append(" ( ");
        boolean first = true;
        for(Object value:values){
            if(first){
                where.append("?");
                first = false;
            }else {
                where.append(",?");
            }
            parameterBindings.addIndexBinding(value);
        }
        where.append(") ");
        return (T)this;
    }


    public T regExp(String column,String regExp){
        return and(column," REGEXP ",regExp);
    }

    public T in(String column, Object[] values){
        return inSqlGenerate(StringUtil.underscoreName(column),values," IN ");
    }

    public T notIn(String column, Object[] values){
        return inSqlGenerate(StringUtil.underscoreName(column),values," NOT IN ");
    }

    public T limit(int limit){
        where.append(" LIMIT ? ");
        parameterBindings.addIndexBinding(limit);
        return (T)this;
    }

    public T asc(String col){
        where.append(" ORDER BY ");
        where.append(StringUtil.underscoreName(col));
        where.append(" ASC ");
        return (T)this;
    }


    public T desc(String col){
        where.append(" ORDER BY ");
        where.append(StringUtil.underscoreName(col));
        where.append(" DESC ");
        return (T)this;
    }


    public T orderBy(String orderBy){
        where.append(" ORDER BY ");
        where.append(StringUtil.underscoreName(orderBy));
        return  (T)this;
    }

    public T groupBy(String groupBy){
        where.append(" GROUP BY ");
        where.append(StringUtil.underscoreName(groupBy));
        return (T) this;
    }

    public T offset(int offset){
        where.append(" OFFSET ");
        parameterBindings.addIndexBinding(offset);
        return (T)this;
    }

    public T or(List<Expr> exprList){
        if(exprList==null||exprList.size()==0){
            return (T)this;
        }
        Expr[] tmp = (Expr[])exprList.toArray();
        return this.or(tmp);
    }

    public T or(Expr... exprArray){
        where.append(" AND (");
        boolean init =true;
        for(Expr expr:exprArray){
            if(init){
                init = false;
            }else{
                where.append(" OR ");
            }
            where.append(expr.getSql());
            parameterBindings.addIndexBinding(expr.getRight());
        }
        where.append(")");
        return (T)this;
    }

}
