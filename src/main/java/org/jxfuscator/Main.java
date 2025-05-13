package org.jxfuscator;

import org.jxfuscator.transformers.Transformer;
import org.jxfuscator.transformers.impl.NumberTransformer;
import org.jxfuscator.transformers.impl.StringTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class Main {
    private static final Map<String, ClassNode> classes = new HashMap<>();
    private static final Map<String, byte[]> resources = new HashMap<>();
    private static final List<Transformer> transformers = new ArrayList<>();
    private static final Map<String, byte[]> originalClassData = new HashMap<>();


    public static void main(String[] args) throws IOException {
        File input = new File("/home/artem/IdeaProjects/obfuscator/jar/untitled.jar");
        File output = new File("/home/artem/IdeaProjects/obfuscator/jar/untitled-obf.jar");
        readInputJar(input);
        addTransformer(new NumberTransformer());
        addTransformer(new StringTransformer());

        applyTransformers();
        writeOutputJar(output);

    }


    private static void readInputJar(File file) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                byte[] data = zin.readAllBytes();
                if (entry.getName().endsWith(".class")) {
                    System.out.println(entry.getName());
                    ClassReader reader = new ClassReader(data);
                    ClassNode node = new ClassNode();
                    reader.accept(node, ClassReader.EXPAND_FRAMES);
                    classes.put(entry.getName(), node);
                    originalClassData.put(entry.getName(), data);
                } else {
                    resources.put(entry.getName(), data);
                }
            }
        }
    }

    private static void applyTransformers() {
        AtomicInteger index = new AtomicInteger();
        for (Map.Entry<String, ClassNode> entry : classes.entrySet()) {
            String name = entry.getKey();
            ClassNode node = entry.getValue();

            for (Transformer transformer : transformers) {
                transformer.process(node);
            }

            index.incrementAndGet();
        }
    }

    private static void writeOutputJar(File file) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
            for (Map.Entry<String, byte[]> res : resources.entrySet()) {
                out.putNextEntry(new ZipEntry(res.getKey()));
                out.write(res.getValue());
            }

            for (Map.Entry<String, ClassNode> entry : classes.entrySet()) {
                String className = entry.getKey();
                ClassNode node = entry.getValue();

                int mode = ClassWriter.COMPUTE_MAXS;


                byte[] classBytes;
                try {
                    ClassWriter writer = new ClassWriter(mode) {
                        @Override
                        protected String getCommonSuperClass(String type1, String type2) {
                            return "java/lang/Object";
                        }
                    };
                    node.accept(writer);
                    classBytes = writer.toByteArray();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                out.putNextEntry(new ZipEntry(className));
                out.write(classBytes);
            }
        }
    }


    public static void addTransformer(Transformer transformer) {
        transformers.add(transformer);
    }

}
