package org.example.shareddocs.collaboration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 操作转换(OT)算法实现
 * 简化版本，仅支持基本的插入和删除操作
 */
@Slf4j
@Component
public class OperationTransform {
    
    /**
     * 转换两个操作
     * @param op1 操作1
     * @param op2 操作2
     * @return 转换后的操作对
     */
    public TransformResult transform(Operation op1, Operation op2) {
        log.debug("转换操作: op1={}, op2={}", op1, op2);
        
        Operation transformedOp1 = transformOperation(op1, op2);
        Operation transformedOp2 = transformOperation(op2, op1);
        
        return new TransformResult(transformedOp1, transformedOp2);
    }
    
    /**
     * 转换单个操作
     */
    private Operation transformOperation(Operation op, Operation otherOp) {
        Operation transformed = op.clone();
        
        if (op.getType() == OperationType.INSERT && otherOp.getType() == OperationType.INSERT) {
            // 两个插入操作
            if (op.getPosition() > otherOp.getPosition() || 
                (op.getPosition().equals(otherOp.getPosition()) && op.getClientId().compareTo(otherOp.getClientId()) > 0)) {
                transformed.setPosition(op.getPosition() + otherOp.getContent().length());
            }
        } else if (op.getType() == OperationType.INSERT && otherOp.getType() == OperationType.DELETE) {
            // 插入 vs 删除
            if (op.getPosition() > otherOp.getPosition()) {
                transformed.setPosition(Math.max(otherOp.getPosition(), 
                    op.getPosition() - otherOp.getLength()));
            }
        } else if (op.getType() == OperationType.DELETE && otherOp.getType() == OperationType.INSERT) {
            // 删除 vs 插入
            if (op.getPosition() >= otherOp.getPosition()) {
                transformed.setPosition(op.getPosition() + otherOp.getContent().length());
            }
        } else if (op.getType() == OperationType.DELETE && otherOp.getType() == OperationType.DELETE) {
            // 两个删除操作
            if (op.getPosition() >= otherOp.getPosition() + otherOp.getLength()) {
                transformed.setPosition(op.getPosition() - otherOp.getLength());
            } else if (op.getPosition() + op.getLength() <= otherOp.getPosition()) {
                // 不重叠，无需调整
            } else {
                // 部分重叠，需要复杂处理（简化：保持原位置）
                log.warn("检测到重叠的删除操作，可能需要更复杂的OT算法");
            }
        }
        
        return transformed;
    }
    
    /**
     * 应用操作到文档内容
     */
    public String applyOperation(String content, Operation operation) {
        if (operation.getType() == OperationType.INSERT) {
            return content.substring(0, operation.getPosition()) + 
                   operation.getContent() + 
                   content.substring(operation.getPosition());
        } else if (operation.getType() == OperationType.DELETE) {
            return content.substring(0, operation.getPosition()) + 
                   content.substring(operation.getPosition() + operation.getLength());
        }
        return content;
    }
    
    /**
     * 转换结果
     */
    @Data
    public static class TransformResult {
        private Operation transformedOp1;
        private Operation transformedOp2;
        
        public TransformResult(Operation transformedOp1, Operation transformedOp2) {
            this.transformedOp1 = transformedOp1;
            this.transformedOp2 = transformedOp2;
        }
    }
}
