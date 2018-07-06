package org.exemodel.exceptions;

/**
 *  @author zp [15951818230@163.com]
 *  use RuntimeException instead SQLException because we can handle exception by SpringMVC or other framework default,
 *  so the code will be clean (^_-)
 */
public class JdbcRuntimeException extends RuntimeException {

    public JdbcRuntimeException(String message) {
        super(message);
    }


    public JdbcRuntimeException(Throwable cause) {
        super(cause);
    }

}
