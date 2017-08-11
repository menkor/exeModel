package org.exemodel.exceptions;

/**
 *  on 15/1/26.
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
