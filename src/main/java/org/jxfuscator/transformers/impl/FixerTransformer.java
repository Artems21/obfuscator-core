package org.jxfuscator.transformers.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.jxfuscator.transformers.Transformer;
import org.objectweb.asm.tree.*;

import java.util.List;

import static org.jxfuscator.Main.LOGGER;
import static org.jxfuscator.transformers.impl.RemaperTransformer.replacedFields;
import static org.jxfuscator.transformers.impl.RemaperTransformer.replacedNames;
import static org.jxfuscator.utils.NodeUtil.toNode;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;

public class FixerTransformer extends Transformer {
    @Override
    public void process(ClassNode node) {
        LOGGER.debug("Start fix " + node.name);


        if (replacedNames.containsKey(node.superName)) {
            node.superName = replacedNames.get(node.superName);
        }

        for (FieldNode fieldNode : node.fields) {
            String klass = fieldNode.desc.replaceFirst("L", "").replace(";", "").trim();
            if (replacedNames.containsKey(klass)) {
                System.out.println(fieldNode.desc);
                fieldNode.desc = "L" + replacedNames.get(klass) + ";";
                System.out.println(fieldNode.desc);

            }
        }


        for (MethodNode methodNode : node.methods) {
            for (AbstractInsnNode insnNode : methodNode.instructions) {
//                if (insnNode instanceof MethodInsnNode methodInsnNode) {
//                    if (replacedNames.containsKey(methodInsnNode.owner)) {
//                        methodNode.instructions.insertBefore(methodInsnNode,
//                                new MethodInsnNode(methodInsnNode.getOpcode(), node.name, methodInsnNode.name, methodInsnNode.desc, methodInsnNode.itf));
//                        methodNode.instructions.remove(methodInsnNode);
//                    }
//                }

                if (insnNode instanceof FieldInsnNode fieldInsnNode) {
//                    if (replacedNames.containsKey(fieldInsnNode.owner)) {
//                        methodNode.instructions.insertBefore(fieldInsnNode,
//                                new FieldInsnNode(fieldInsnNode.getOpcode(), node.name, fieldInsnNode.name,
//                                        replacedNames.containsKey(fieldInsnNode.desc.replaceFirst("L", "").replace(";", "")) ?  "L" + replacedNames.get(fieldInsnNode.desc.replaceFirst("L", "").replace(";", "")) + ";" : fieldInsnNode.desc
//                                ));
//                        methodNode.instructions.remove(fieldInsnNode);
//                    }
                    methodNode.instructions.insertBefore(fieldInsnNode,
                            new FieldInsnNode(fieldInsnNode.getOpcode(), fieldInsnNode.owner, getChangedName(node, fieldInsnNode), fieldInsnNode.desc));
                    methodNode.instructions.remove(fieldInsnNode);

                }

//                if (insnNode instanceof TypeInsnNode typeInsnNode) {
//                    if (replacedNames.containsKey(typeInsnNode.desc)) {
//                        System.out.println(typeInsnNode.desc);
//                        methodNode.instructions.insertBefore(typeInsnNode,
//                                new TypeInsnNode(typeInsnNode.getOpcode(), replacedNames.get(typeInsnNode.desc)));
//                        methodNode.instructions.remove(typeInsnNode);
//                    }
//                }
            }
        }
    }

    private String getChangedName(ClassNode node, FieldInsnNode fieldInsnNode) {

        String origName = fieldInsnNode.name;
        String className = fieldInsnNode.owner;

        if (replacedFields.containsKey(origName)) {
            List<Pair<String, String>> list = replacedFields.get(origName);
            LOGGER.debug("Check field " + origName);
            //LOGGER.debug(className);

            for (Pair<String, String> pair : list) {
                if (pair.getKey().equals(className)) {
                    LOGGER.debug("Fix field use " + fieldInsnNode.name + " -> " + pair.getValue());
                    return pair.getValue();
                }

//                    if (checkClass.interfaces != null) {
//                        LOGGER.debug("Check interfaces " + checkClass.interfaces);
//                        for (String interfaceName : checkClass.interfaces) {
//                            if (pair.getKey().equals(interfaceName)) {
//                                LOGGER.debug("Fix field use " + fieldInsnNode.name + " -> " + pair.getValue());
//                                return pair.getValue();
//                            }
//                        }
//                    }
            }

            ClassNode checkClass = toNode(className);
            if (checkClass != null) {
                if (checkClass.superName != null) {
                    LOGGER.debug("Check superclasses " + checkClass.superName);
                    for (Pair<String, String> pair : list) {

                        if (pair.getKey().equals(checkClass.superName)) {
                            LOGGER.debug("Fix field use " + fieldInsnNode.name + " -> " + pair.getValue());
                            return pair.getValue();
                        }
                    }
                }
            }
        }


        // CaseOpenManagerImpl
        // CaseOpenManager
        // SimpleCache
        return fieldInsnNode.name;
    }

//    private String recursiveFind(Pair<String, String> pair, ClassNode node) {
//
//        if (node.superName != null) {
//            LOGGER.debug("Check superclasses " + checkClass.superName);
//            if (pair.getKey().equals(checkClass.superName)) {
//                LOGGER.debug("Fix field use " + fieldInsnNode.name + " -> " + pair.getValue());
//                return pair.getValue();
//            }
//        }
//        return null;
//    }
}
