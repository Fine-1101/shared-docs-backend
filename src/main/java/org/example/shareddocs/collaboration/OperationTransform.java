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
        } else if (op.getType() == OperationType.INSERT && otherOp.getType() == OperationType.REPLACE) {
            // 插入 vs 替换（替换=删除+插入）
            if (op.getPosition() >= otherOp.getPosition() + otherOp.getLength()) {
                // 插入位置在替换区域之后，调整位置
                int lengthDiff = (otherOp.getContent() != null ? otherOp.getContent().length() : 0) - otherOp.getLength();
                transformed.setPosition(op.getPosition() + lengthDiff);
            } else if (op.getPosition() > otherOp.getPosition()) {
                // 插入位置在替换区域内，调整到替换开始位置
                transformed.setPosition(otherOp.getPosition());
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
        } else if (op.getType() == OperationType.DELETE && otherOp.getType() == OperationType.REPLACE) {
            // 删除 vs 替换
            if (op.getPosition() >= otherOp.getPosition() + otherOp.getLength()) {
                // 删除在替换之后，调整位置
                int lengthDiff = (otherOp.getContent() != null ? otherOp.getContent().length() : 0) - otherOp.getLength();
                transformed.setPosition(op.getPosition() + lengthDiff);
            } else if (op.getPosition() + op.getLength() <= otherOp.getPosition()) {
                // 删除在替换之前，无需调整
            } else {
                // 重叠情况，简化处理
                log.warn("检测到删除与替换重叠，简化处理");
                transformed.setPosition(otherOp.getPosition());
            }
        } else if (op.getType() == OperationType.REPLACE && otherOp.getType() == OperationType.INSERT) {
            // 替换 vs 插入
            if (op.getPosition() >= otherOp.getPosition()) {
                transformed.setPosition(op.getPosition() + otherOp.getContent().length());
            }
        } else if (op.getType() == OperationType.REPLACE && otherOp.getType() == OperationType.DELETE) {
            // 替换 vs 删除
            if (op.getPosition() >= otherOp.getPosition() + otherOp.getLength()) {
                transformed.setPosition(op.getPosition() - otherOp.getLength());
            } else if (op.getPosition() + op.getLength() <= otherOp.getPosition()) {
                // 不重叠
            } else {
                log.warn("检测到替换与删除重叠，简化处理");
            }
        } else if (op.getType() == OperationType.REPLACE && otherOp.getType() == OperationType.REPLACE) {
            // 两个替换操作
            if (op.getPosition() >= otherOp.getPosition() + otherOp.getLength()) {
                // 第一个替换在第二个之后
                int lengthDiff = (otherOp.getContent() != null ? otherOp.getContent().length() : 0) - otherOp.getLength();
                transformed.setPosition(op.getPosition() + lengthDiff);
            } else if (op.getPosition() + op.getLength() <= otherOp.getPosition()) {
                // 不重叠，无需调整
            } else {
                // 重叠的替换操作，需要特殊处理
                log.warn("检测到重叠的替换操作，简化处理");
                // 简化：保持位置不变，让应用层处理冲突
            }
        }
        
        return transformed;
    }
    
    /**
     * 应用操作到文档内容
     */
    public String applyOperation(String content, Operation operation) {
        if (content == null) {
            content = "";
        }
        
        if (operation.getType() == OperationType.INSERT) {
            int pos = Math.min(operation.getPosition(), content.length());
            return content.substring(0, pos) + 
                   operation.getContent() + 
                   content.substring(pos);
        } else if (operation.getType() == OperationType.DELETE) {
            int start = Math.min(operation.getPosition(), content.length());
            int end = Math.min(start + (operation.getLength() != null ? operation.getLength() : 0), content.length());
            return content.substring(0, start) + 
                   content.substring(end);
        } else if (operation.getType() == OperationType.REPLACE) {
            int start = Math.min(operation.getPosition(), content.length());
            int end = Math.min(start + (operation.getLength() != null ? operation.getLength() : 0), content.length());
            String replaceContent = operation.getContent() != null ? operation.getContent() : "";
            return content.substring(0, start) + 
                   replaceContent + 
                   content.substring(end);
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
