package org.exemodel.exceptions;

/**
 * @author zp [15951818230@163.com]
 */
public class JedisRuntimeException extends RuntimeException
{
    public JedisRuntimeException(String message) {
        super(message);
    }

    public JedisRuntimeException(Throwable cause){ super(cause);}
}
