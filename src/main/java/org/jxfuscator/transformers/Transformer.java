package org.jxfuscator.transformers;

import org.objectweb.asm.tree.ClassNode;

public abstract class Transformer {
    public abstract void process(ClassNode node);
    protected boolean isUsed = false;
    public void setUsedTrue() {
        isUsed = true;
    }
}
