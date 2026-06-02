package org.example.shareddocs.common.exception;

/**
 * 资源不存在异常
 */
public class NotFoundException extends BusinessException {
    
    public NotFoundException(String message) {
        super(404, message);
    }
}
