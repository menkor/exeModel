package org.exemodel.builder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exemodel.session.impl.JdbcSession;
import org.exemodel.util.Expr;
import org.exemodel.util.ParameterBindings;
import org.exemodel.util.StringUtil;
import org.exemodel.util.Supplier;

import java.util.Collection;
import java.util.List;

/**
 * @author zp [15951818230@163.com]
 */
@SuppressWarnings("unchecked")
public abstract class SqlBuilder<T> {
    private final static Log logger = LogFactory.getLog(SqlBuilder.class);
    protected ParameterBindings parameterBindings = new ParameterBindings();
    protected StringBuilder where = new StringBuilder(" WHERE (1=1)");

    public T and(String column, String op, Object value) {
        where.append(" AND ");
        where.append(StringUtil.underscoreName(column));
        where.append(op);
        where.append(" ? ");
        parameterBindings.addIndexBinding(value);
        return (T) this;
    }

    public T andWhenNotNull(String column, String operation, Object value) {
        if (value == null) {
            return (T) this;
        }
        return and(column, operation, value);
    }


    /**
     * and when supplier.get not null,ignore if null
     *
     * @param column
     * @param operation
     * @param supplier
     * @return
     */
    public T andWhenNotNull(String column, String operation, Supplier supplier) {
        Object value = supplier.get();
        if (value == null) {
            return (T) this;
        }
        return and(column, operation, value);
    }

    /**
     * equal
     */
    public T eq(String column, Object value) {
        return (T) and(column, "=", value);
    }


    /**
     * not equal
     */
    public T ne(String column, Object value) {
        return and(column, "<>", value);
    }


    /**
     * like
     */
    public T like(String column, Object value) {
        return and(column, " LIKE ", value);
    }


    /**
     * state equal
     */
    public T state(Object value) {
        return and("state", "=", value);
    }

    /**
     * greater than
     */
    public T gt(String column, Object value) {
        return and(column, ">", value);
    }

    /**
     * less than
     */
    public T lt(String column, Object value) {
        return and(column, "<", value);
    }


    /**
     * less than or equal
     */
    public T le(String column, Object value) {
        return and(column, "<=", value);
    }

    /**
     * greater than or equal
     */
    public T ge(String column, Object value) {
        return and(column, ">=", value);
    }


    public T between(String column, Object from, Object to) {
        where.append(" AND (");
        where.append(column);
        where.append(" BETWEEN ? AND ? )");
        parameterBindings.addIndexBinding(from).addIndexBinding(to);
        return (T) this;
    }

    /**
     * column is null
     *
     * @param column
     * @return
     */
    public T isNull(String column) {
        where.append(" AND ");
        where.append(StringUtil.underscoreName(column));
        where.append(" IS NULL ");
        return (T) this;
    }

    /**
     * column is not null
     *
     * @param column
     * @return
     */
    public T isNotNull(String column) {
        where.append(" AND ");
        where.append(StringUtil.underscoreName(column));
        where.append(" IS NOT NULL ");
        return (T) this;
    }


    public T in(String column, Collection values) {
        return inSqlGenerate(column, values, " IN ");
    }


    public T notIn(String column, Collection values) {
        return inSqlGenerate(column, values, " NOT IN ");
    }


    public T regExp(String column, String regExp) {
        return and(column, " REGEXP ", regExp);
    }

    public T in(String column, Object[] values) {

        return inSqlGenerate(StringUtil.underscoreName(column), values, " IN ");
    }

    public T notIn(String column, Object[] values) {
        if (values == null || values.length == 0) {
            return (T) this;
        }
        return inSqlGenerate(StringUtil.underscoreName(column), values, " NOT IN ");
    }

    public T limit(int limit) {
        where.append(" LIMIT ? ");
        parameterBindings.addIndexBinding(limit);
        return (T) this;
    }

    public T limit(int offset, int limit) {
        where.append(" LIMIT ?,? ");
        parameterBindings.addIndexBinding(offset);
        parameterBindings.addIndexBinding(limit);
        return (T) this;
    }

    public T asc(String col) {
        where.append(" ORDER BY ");
        where.append(StringUtil.underscoreName(col));
        where.append(" ASC ");
        return (T) this;
    }


    public T desc(String col) {
        where.append(" ORDER BY ");
        where.append(StringUtil.underscoreName(col));
        where.append(" DESC ");
        return (T) this;
    }


    public T orderBy(String columns) {
        where.append(" ORDER BY ");
        where.append(StringUtil.underscoreName(columns));
        return (T) this;
    }

    public T groupBy(String columns) {
        where.append(" GROUP BY ");
        where.append(StringUtil.underscoreName(columns));
        return (T) this;
    }

    public T offset(int offset) {
        where.append(" OFFSET ? ");
        parameterBindings.addIndexBinding(offset);
        return (T) this;
    }

    public T or(List<Expr> exprList) {
        if (exprList == null || exprList.size() == 0) {
            return (T) this;
        }
        Expr[] tmp = (Expr[]) exprList.toArray();
        return this.or(tmp);
    }

    public T or(Expr... exprArray) {
        if (exprArray == null || exprArray.length == 0) {
            return (T) this;
        }
        int i = 0;
        for (Expr expr : exprArray) {
            if (expr != null) {
                break;
            }
            i++;
        }
        if (i == exprArray.length) {
            return (T) this;
        }

        where.append(" AND (");
        boolean init = true;
        for (Expr expr : exprArray) {
            if (expr == null) {
                continue;
            }
            if (init) {
                init = false;
            } else {
                where.append(" OR ");
            }
            where.append(expr.getSql());
            parameterBindings.extend(expr.getParameterBindings());
        }
        where.append(")");
        return (T) this;
    }


    private T inSqlGenerate(String column, Object[] values, String op) {
        if (values == null || values.length == 0) {
            where.append(" AND 0 = 1 ");
            return (T) this;
        }
        where.append(" AND ");
        where.append(Expr.inSqlGenerator(op, column, values, parameterBindings));
        return (T) this;
    }

    private T inSqlGenerate(String column, Collection list, String op) {

        Object[] values = new Object[list.size()];
        int i = 0;
        for (Object o : list) {
            values[i++] = o;
        }
        return inSqlGenerate(column, values, op);
    }


}
