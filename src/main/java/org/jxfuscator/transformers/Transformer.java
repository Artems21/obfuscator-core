package org.jxfuscator.transformers;

import org.objectweb.asm.tree.ClassNode;

public abstract class Transformer {
    public abstract boolean process(ClassNode node);
}
