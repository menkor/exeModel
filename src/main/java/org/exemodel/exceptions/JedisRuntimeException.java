package org.exemodel.exceptions;

/**
 * Created by zp on 16/10/9.
 */
public class JedisRuntimeException extends RuntimeException
{
    public JedisRuntimeException(String message) {
        super(message);
    }

    public JedisRuntimeException(Throwable cause){ super(cause);}
}
