package org.jxfuscator.transformers.impl;

import org.jxfuscator.logger.Logger;
import org.jxfuscator.transformers.Transformer;
import org.jxfuscator.utils.Trio;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.jxfuscator.utils.NodeUtil.*;
import static org.jxfuscator.utils.RandomUtil.randomInt;
import static org.objectweb.asm.Opcodes.*;

public class NumberTransformer extends Transformer {
    private static final Logger LOGGER = new Logger(NumberTransformer.class.getSimpleName());

    @Override
    public void process(ClassNode node) {
        if ((node.access & ACC_INTERFACE) != 0)
            return;
        List<Trio> vm_cases = new ArrayList<>();
        for (MethodNode methodNode : node.methods) {


            if (methodNode.name.contains("decrypt") || methodNode.name.contains("clinit")) {
                for (AbstractInsnNode insnNode : methodNode.instructions) {
                    if (isIntegerNumber(insnNode)) {
                        int value = getIntValue(insnNode);
                        if (value == Integer.MIN_VALUE) {
                            continue;
                        }
                        int a = randomInt(1500, Integer.MAX_VALUE);
                        int b = a ^ value;
                        methodNode.instructions.insertBefore(insnNode, generateIntPush(a));
                        methodNode.instructions.insertBefore(insnNode, generateIntPush(b));
                        methodNode.instructions.insertBefore(insnNode, new InsnNode(IXOR));
                        methodNode.instructions.remove(insnNode);
                    }
                }
                continue;
            }


            for (AbstractInsnNode insnNode : methodNode.instructions) {
                if (isIntegerNumber(insnNode)) {
                    int value = getIntValue(insnNode);
                    if (value == Integer.MIN_VALUE) {
                        continue;
                    }
                    LOGGER.info("Get value: " + value);

                    int a = randomInt(1500, Integer.MAX_VALUE);
                    int b = randomInt(1500, Integer.MAX_VALUE);
                    vm_cases.add(new Trio(a, b, value));

                    methodNode.instructions.insertBefore(insnNode, generateIntPush(a));
                    methodNode.instructions.insertBefore(insnNode, generateIntPush(b));

                    methodNode.instructions.insertBefore(insnNode, new MethodInsnNode(INVOKESTATIC,
                            node.name,
                            "h" + node.name + "NUM_VM",
                            "(II)I",
                            false));


                    methodNode.instructions.remove(insnNode);

                }
            }
        }

        if (!vm_cases.isEmpty()) {
            vm_cases.sort(Comparator.comparingInt(trio -> trio.x ^ trio.y));
            int[] switch_values = new int[vm_cases.size()];
            LabelNode[] labels = new LabelNode[vm_cases.size()];
            for (int i = 0; i < vm_cases.size(); i++) {
                Trio trio = vm_cases.get(i);
                switch_values[i] = trio.x ^ trio.y;
                labels[i] = new LabelNode();
            }

            LabelNode labelDefault = new LabelNode();

            String methodName = "h" + node.name + "NUM_VM";
            MethodNode mn = new MethodNode(ACC_PUBLIC | ACC_STATIC, methodName, "(II)I", null, null);

            mn.instructions.add(new VarInsnNode(ILOAD, 0));
            mn.instructions.add(new VarInsnNode(ILOAD, 1));
            mn.instructions.add(new InsnNode(IXOR));
            mn.instructions.add(new VarInsnNode(ISTORE, 2));
            mn.instructions.add(new VarInsnNode(ILOAD, 2));
            mn.instructions.add(new LookupSwitchInsnNode(labelDefault, switch_values, labels));


            for (int i = 0; i < vm_cases.size(); i++) {
                Trio trio = vm_cases.get(i);
                LabelNode label = labels[i];
                mn.instructions.add(label);
                mn.visitFrame(Opcodes.F_SAME, 0, null, 0, null);


                int a = randomInt(1500, Integer.MAX_VALUE);
                int b = a ^ trio.z;

                if (a >= -1 && a <= 5) {
                    mn.instructions.add(new InsnNode(a + 3));
                } else if (a >= -128 && a <= 127) {
                    mn.instructions.add(new IntInsnNode(BIPUSH, a));
                } else if (a >= -32768 && a <= 32767) {
                    mn.instructions.add(new IntInsnNode(SIPUSH, a));
                } else {
                    mn.instructions.add(new LdcInsnNode(a));
                }

                if (b >= -1 && b <= 5) {
                    mn.instructions.add(new InsnNode(b + 3));
                } else if (b >= -128 && b <= 127) {
                    mn.instructions.add(new IntInsnNode(BIPUSH, b));
                } else if (b >= -32768 && b <= 32767) {
                    mn.instructions.add(new IntInsnNode(SIPUSH, b));
                } else {
                    mn.instructions.add(new LdcInsnNode(b));
                }

                mn.instructions.add(new InsnNode(IXOR));
                mn.instructions.add(new VarInsnNode(ISTORE, 1));
                mn.instructions.add(new VarInsnNode(ILOAD, 1));
                mn.instructions.add(new InsnNode(IRETURN));
            }

            mn.instructions.add(labelDefault);
            mn.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mn.instructions.add(new InsnNode(ICONST_0));
            mn.instructions.add(new InsnNode(IRETURN));
            mn.visitMaxs(3, 3);
            node.methods.add(mn);

        }
    }
}
