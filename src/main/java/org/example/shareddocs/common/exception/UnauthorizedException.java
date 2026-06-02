package org.example.shareddocs.common.exception;

/**
 * 未授权异常
 */
public class UnauthorizedException extends BusinessException {
    
    public UnauthorizedException(String message) {
        super(401, message);
    }
}
