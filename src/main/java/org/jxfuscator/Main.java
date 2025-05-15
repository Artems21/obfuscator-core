package org.jxfuscator;

import org.jxfuscator.logger.Logger;
import org.jxfuscator.transformers.Transformer;
import org.jxfuscator.transformers.impl.*;
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

import static org.jxfuscator.transformers.impl.RemaperTransformer.replacedNames;


public class Main {
    public static final Map<String, ClassNode> classes = new HashMap<>();
    private static final Map<String, byte[]> resources = new HashMap<>();

    private static final List<Transformer> transformers = new ArrayList<>();
    private static final List<Transformer> remapers = new ArrayList<>();


    private static final Map<String, byte[]> originalClassData = new HashMap<>();

    private static boolean NeedReplaceMainClass = false;

    public static final Logger LOGGER = new Logger();


    public static void main(String[] args) throws IOException {
        File input = new File("/home/artem/IdeaProjects/obfuscator/jar/untitled.jar");
        File output = new File("/home/artem/IdeaProjects/obfuscator/jar/untitled-obf.jar");
        readInputJar(input);

        addTransformer(new NumberTransformer());
        addTransformer(new StringTransformer());
        addTransformer(new IXORTransformer());
        addTransformer(new LocalsRenameTransformer());

         addTransformer(new RemaperTransformer());

        applyTransformers();
        addRemapers(new FixerTransformer());
        applyRemapers();
  //      needReplaceMainClass();

        writeOutputJar(output);

    }

    private static void needReplaceMainClass() {
        NeedReplaceMainClass = true;
    }

    private static void readInputJar(File file) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                byte[] data = zin.readAllBytes();
                if (entry.getName().endsWith(".class")) {
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
            LOGGER.debug("Process: " + name);


            for (Transformer transformer : transformers) {
                transformer.process(node);
            }

            index.incrementAndGet();
        }
    }

    private static void applyRemapers() {
        AtomicInteger index = new AtomicInteger();
        for (Map.Entry<String, ClassNode> entry : classes.entrySet()) {
            String name = entry.getKey();
            ClassNode node = entry.getValue();

            for (Transformer transformer : remapers) {
                transformer.process(node);
            }

            index.incrementAndGet();
        }
    }

    private static void writeOutputJar(File file) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
            for (Map.Entry<String, byte[]> res : resources.entrySet()) {
                if (res.getKey().contains("plugin.yml") && NeedReplaceMainClass) {
                    out.putNextEntry(new ZipEntry(res.getKey()));
                    String config = new String(res.getValue());
                    String main_class = Arrays.stream(config.split("\n"))
                            .filter(s -> s.contains("main: "))
                            .toList()
                            .get(0)
                            .replace("main: ", "")
                            .trim();
                    if (replacedNames.get(main_class.replace(".", "/")) == null) {
                        throw new RuntimeException("error when try replace main class");
                    }
                    config = config.replace(main_class, replacedNames.get(main_class.replace(".", "/")).replace("/", "."));

                    out.write(config.getBytes());
                    continue;
                }
//                if (res.getKey().contains("MANIFEST.MF") && NeedReplaceMainClass) {
//                    out.putNextEntry(new ZipEntry(res.getKey()));
//                    String config = new String(res.getValue());
//                    String main_class = Arrays.stream(config.split("\n"))
//                            .filter(s -> s.contains("Main-Class: "))
//                            .toList()
//                            .get(0)
//                            .replace("Main-Class: ", "")
//                            .trim();
//                    if (replacedNames.get(main_class.replace(".", "/")) == null) {
//                        throw new RuntimeException("error when try replace main class");
//                    }
//                    config = config.replace(main_class, replacedNames.get(main_class.replace(".", "/")));
//
//                    out.write(config.getBytes());
//                    continue;
//                }

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

                if (replacedNames.containsKey(className.replace(".class", ""))) {
                    out.putNextEntry(new ZipEntry(replacedNames.get(className.replace(".class", "")) + ".class"));
                    out.write(classBytes);
                    continue;
                }
                out.putNextEntry(new ZipEntry(className));
                out.write(classBytes);
            }
        }
    }


    public static void addTransformer(Transformer transformer) {
        transformers.add(transformer);
    }

    public static void addRemapers(Transformer transformer) {
        remapers.add(transformer);
    }

}
