package org.jxfuscator.transformers.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.jxfuscator.transformers.Transformer;
import org.jxfuscator.utils.NodeUtil;
import org.jxfuscator.utils.Trio;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

import static org.jxfuscator.utils.NodeUtil.generateIntPush;
import static org.jxfuscator.utils.RandomUtil.randomInt;
import static org.objectweb.asm.Opcodes.*;

public class StringTransformer extends Transformer {
    @Override
    public void process(ClassNode node) {
        if ((node.access & ACC_INTERFACE) != 0)
            return;
        List<Pair<String, Integer>> strings = new ArrayList<>();
        InsnList instructions = new InsnList();
        for (MethodNode methodNode : node.methods) {
            for (AbstractInsnNode insnNode : methodNode.instructions) {
                if (insnNode instanceof LdcInsnNode) {
                    LdcInsnNode ldcInsnNode = (LdcInsnNode) insnNode;
                    if (ldcInsnNode.cst instanceof String) {
                        String str = (String) ldcInsnNode.cst;
                        if (str.isEmpty()) {
                            continue;
                        }
                        setUsedTrue();

                        int id = randomInt(1500, Integer.MAX_VALUE); // колизии :///
                        strings.add(Pair.of(str, id));

                        byte[] base = str.getBytes();

                        byte[] first = new byte[base.length];
                        byte[] second = new byte[base.length];

                        for (int i = 0; i < base.length; i++) {
                            first[i] = (byte) randomInt(0, (int) base[i]);
                            second[i] = (byte) (base[i] - first[i]);
                        }

                        String fieldName = "first_" + id;
                        FieldNode fieldNode = new FieldNode(ACC_PRIVATE | ACC_STATIC, fieldName, "[B", null, null);
                        node.fields.add(fieldNode);

                        // generate instructions in clinit
                        instructions.add(generateIntPush(first.length));
                        instructions.add(new IntInsnNode(Opcodes.NEWARRAY, T_BYTE));
                        instructions.add(new InsnNode(DUP));
                        for (int i = 0; i < base.length; i++) {
                            instructions.add(generateIntPush(i));
                            instructions.add(generateIntPush(first[i]));
                            instructions.add(new InsnNode(BASTORE));
                            if (i != base.length - 1)
                                instructions.add(new InsnNode(DUP));
                        }
                        instructions.add(new FieldInsnNode(
                                Opcodes.PUTSTATIC,
                                node.name,
                                fieldName,
                                "[B"
                        ));

                        //generate second
                        InsnList secondList = new InsnList();
                        secondList.add(generateIntPush(first.length));
                        secondList.add(new IntInsnNode(Opcodes.NEWARRAY, T_BYTE));
                        secondList.add(new InsnNode(DUP));
                        for (int i = 0; i < base.length; i++) {
                            secondList.add(generateIntPush(i));
                            secondList.add(generateIntPush(second[i]));
                            secondList.add(new InsnNode(BASTORE));
                            if (i != base.length - 1)
                                secondList.add(new InsnNode(DUP));
                        }

                        secondList.add(new MethodInsnNode(INVOKESTATIC,
                                node.name,
                                "decrypt_" + id,
                                "([B)Ljava/lang/String;",
                                false));

                        methodNode.instructions.insertBefore(insnNode, secondList);
                        methodNode.instructions.remove(insnNode);
                    }
                }
            }
        }
        if (this.isUsed) {

            // create static {}
            MethodNode clInit = NodeUtil.getMethod(node, "<clinit>");
            if (clInit == null) {
                clInit = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, new String[0]);
                node.methods.add(clInit);
            }
            if (clInit.instructions == null)
                clInit.instructions = new InsnList();

            if (clInit.instructions.getFirst() == null) {
                clInit.instructions.add(instructions);
                clInit.instructions.add(new InsnNode(RETURN));
            } else {
                clInit.instructions.insertBefore(clInit.instructions.getFirst(), instructions);
            }


            //create methods
            for (Pair<String, Integer> pair : strings) {
                String str = pair.getKey();
                int id = pair.getValue();
                System.out.println(str);
                String fieldName = "first_" + id;
                String methodName = "decrypt_" + id;

                MethodNode mn = new MethodNode(ACC_PUBLIC | ACC_STATIC, methodName, "([B)Ljava/lang/String;", null, null);
                mn.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
                mn.instructions.add(new InsnNode(Opcodes.DUP));
                mn.instructions.add(new MethodInsnNode(
                        Opcodes.INVOKESPECIAL,
                        "java/lang/StringBuilder",
                        "<init>",
                        "()V",
                        false
                ));
                mn.instructions.add(new VarInsnNode(Opcodes.ASTORE, 1));

                for (int i = 0; i < str.length(); i++) {
                    mn.instructions.add(new VarInsnNode(ALOAD, 1)); // string builder
                    mn.instructions.add(new VarInsnNode(ALOAD, 0)); // [B in arg0
                    mn.instructions.add(generateIntPush(i)); // element index
                    mn.instructions.add(new InsnNode(BALOAD));
                    mn.instructions.add(new FieldInsnNode(
                            Opcodes.GETSTATIC,
                            node.name,
                            fieldName,
                            "[B"
                    ));  // get field
                    mn.instructions.add(generateIntPush(i)); // element index
                    mn.instructions.add(new InsnNode(BALOAD));
                    mn.instructions.add(new InsnNode(IADD));
                    mn.instructions.add(new InsnNode(I2C));
                    mn.instructions.add(new MethodInsnNode(
                            INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(C)Ljava/lang/StringBuilder;",
                            false
                    ));
                    mn.instructions.add(new InsnNode(POP));
                }

                mn.instructions.add(new VarInsnNode(ALOAD, 1));
                mn.instructions.add(new MethodInsnNode(
                        INVOKEVIRTUAL,
                        "java/lang/StringBuilder",
                        "toString",
                        "()Ljava/lang/String;",
                        false
                ));
                mn.instructions.add(new InsnNode(ARETURN));
                node.methods.add(mn);

            }
        }
    }
}
