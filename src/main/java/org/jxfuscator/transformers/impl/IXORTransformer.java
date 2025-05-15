package org.jxfuscator.transformers.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.jxfuscator.transformers.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class IXORTransformer extends Transformer {
    @Override
    public void process(ClassNode node) {
        if ((node.access & ACC_INTERFACE) != 0)
            return;
        String methodName = "XOR_" + node.name.replace("/", "_");
        for (MethodNode methodNode : node.methods) {
            if (methodNode.name.contains("XOR"))
                return;
            for (AbstractInsnNode insnNode : methodNode.instructions) {
                if (insnNode.getOpcode() == IXOR) {
                    setUsedTrue();

                    methodNode.instructions.insertBefore(insnNode, new MethodInsnNode(INVOKESTATIC,
                            node.name,
                            methodName,
                            "(II)I",
                            false));


                    methodNode.instructions.remove(insnNode);
                }
            }
        }
        if (isUsed) {
            MethodNode methodNode = new MethodNode(ACC_PRIVATE | ACC_STATIC, methodName, "(II)I", null, null);
            LabelNode start = new LabelNode();
            LabelNode end = new LabelNode();

            methodNode.instructions.add(start);

            methodNode.instructions.add(new VarInsnNode(ILOAD, 0));
            methodNode.instructions.add(new VarInsnNode(ILOAD, 1));

            methodNode.instructions.add(new VarInsnNode(Opcodes.ILOAD, 0));
            methodNode.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
            methodNode.instructions.add(new InsnNode(Opcodes.IOR));
            methodNode.instructions.add(new VarInsnNode(Opcodes.ILOAD, 0));
            methodNode.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
            methodNode.instructions.add(new InsnNode(Opcodes.IAND));
            methodNode.instructions.add(new InsnNode(Opcodes.ICONST_M1));
            methodNode.instructions.add(new InsnNode(Opcodes.IXOR));
            methodNode.instructions.add(new InsnNode(Opcodes.IAND));
            methodNode.instructions.add(new InsnNode(Opcodes.IRETURN));

            methodNode.instructions.add(end);

            methodNode.localVariables.add(new LocalVariableNode("a", "I", null, start, end, 0));
            methodNode.localVariables.add(new LocalVariableNode("b", "I", null, start, end, 1));

            methodNode.parameters = Arrays.asList(
                    new ParameterNode("a", 0),
                    new ParameterNode("b", 0)
            );

            node.methods.add(methodNode);
        }
    }
}
