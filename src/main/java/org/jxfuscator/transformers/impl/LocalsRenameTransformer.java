package org.jxfuscator.transformers.impl;

import org.checkerframework.checker.units.qual.N;
import org.jxfuscator.transformers.Transformer;
import org.jxfuscator.utils.NameGen;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import static org.jxfuscator.utils.RandomUtil.randomInt;

public class LocalsRenameTransformer extends Transformer {
    @Override
    public void process(ClassNode node) {
        for(MethodNode method : node.methods) {

            if(method.localVariables != null && !method.localVariables.isEmpty()) {
                for(LocalVariableNode var : method.localVariables) {
                    var.name = NameGen.jx() + NameGen.String(40);
                }
            }
            if(method.parameters != null && !method.parameters.isEmpty()) {
                for(ParameterNode var : method.parameters) {
                    var.name = NameGen.jx() + NameGen.String(40);
                }
            }
        }
    }
}
