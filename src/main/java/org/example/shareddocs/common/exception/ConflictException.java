package org.example.shareddocs.common.exception;

/**
 * 冲突异常
 */
public class ConflictException extends BusinessException {
    
    public ConflictException(String message) {
        super(409, message);
    }
}
