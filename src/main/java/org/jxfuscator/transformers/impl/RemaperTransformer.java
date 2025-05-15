package org.jxfuscator.transformers.impl;

import eu.decentsoftware.holograms.api.utils.scheduler.S;
import org.apache.commons.lang3.tuple.Pair;
import org.jxfuscator.transformers.Transformer;
import org.jxfuscator.utils.NameGen;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jxfuscator.Main.LOGGER;
import static org.jxfuscator.utils.RandomUtil.randomInt;
import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;

public class RemaperTransformer extends Transformer {

    public static HashMap<String, String> replacedNames = new HashMap<>();

    public static HashMap<String, String> replacedClasses = new HashMap<>();

    public static HashMap<String, String> replacedMethods = new HashMap<>();

    public static Map<String, List<Pair<String, String>>> replacedFields = new HashMap<>();



    @Override
    public void process(ClassNode node) {
        if ((node.access & ACC_INTERFACE) != 0)
            return;
//        if ((node.access & ACC_ABSTRACT) != 0)
//            return;
        //method remap
//        if (node.superName != null)
//            if (!node.superName.equals("java/lang/Object"))
//                return;
//        if (node.interfaces != null)
//           if (!node.interfaces.isEmpty())
//               return;
//        for (MethodNode methodNode : node.methods) {
//            String origName = methodNode.name;
//            if (origName.contains("<") || origName.equals("main") || origName.contains("super"))
//                continue;
//            if (methodNode.visibleAnnotations != null)
//                for (AnnotationNode annotationNode : methodNode.visibleAnnotations) {
//                    System.out.println(annotationNode.desc);
//                }
//
//            //String replaced = NameGen.String(4);
//            String replaced = "m" + origName;
//
//            methodNode.name = replaced;
//            replacedNames.put(node.name + origName, replaced);
//        }

//        // class remap
//        String origName = node.name;
//        //         String replace = NameGen.String(20);
//        String replace = origName + "CCC";
//
//        System.out.println("Remap: "+ origName);
//        node.name = replace;
//
//        replacedNames.put(origName, replace);


//        if (node.superName != null)
//            if (!node.superName.equals("java/lang/Object"))
//                return;
//        if (node.interfaces != null)
//           if (!node.interfaces.isEmpty())
//               return;

        // fields remap
        for (FieldNode fieldNode : node.fields) {
            String origName = fieldNode.name;
            String replace = NameGen.String(20);
            //String replace = "f" + origName + randomInt(1500, Integer.MAX_VALUE);

            LOGGER.debug("Remap field: " + origName + " in " + node.name);
            fieldNode.name = replace;

            put(origName, Pair.of(node.name, replace));
        }

    }
    public void put(String key, Pair<String, String> value) {
        replacedFields.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }
}
