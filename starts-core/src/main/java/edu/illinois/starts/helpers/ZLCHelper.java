/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

 package edu.illinois.starts.helpers;

 
 import java.io.File;
 import java.io.IOException;
 import java.net.URL;
 import java.nio.charset.Charset;
 import java.nio.file.Files;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.logging.Level;
 import java.util.stream.Collectors;
 import java.util.*;
 import java.nio.file.*;
 import java.io.FileInputStream;
 import java.io.StringWriter;
 import java.net.MalformedURLException;
 import java.io.PrintWriter;
 import java.io.FileWriter;

 
 import edu.illinois.starts.constants.StartsConstants;
 import edu.illinois.starts.data.ZLCData;
 import edu.illinois.starts.data.ZLCFileContent;
 import edu.illinois.starts.data.ZLCFormat;
 import edu.illinois.starts.util.ChecksumUtil;
 import edu.illinois.starts.util.Logger;
 import edu.illinois.starts.util.Pair;
 import org.ekstazi.util.Types;
 import org.objectweb.asm.ClassReader;
 import org.objectweb.asm.Label;
 import org.objectweb.asm.Opcodes;
 import org.objectweb.asm.tree.ClassNode;
 import org.objectweb.asm.tree.MethodNode;
 import org.objectweb.asm.util.Printer;
 import org.objectweb.asm.util.Textifier;
 import org.objectweb.asm.util.TraceMethodVisitor;
 
 
 import static edu.illinois.starts.smethods.MethodLevelStaticDepsBuilder.main;
 import static edu.illinois.starts.smethods.MethodLevelStaticDepsBuilder.test2methods;
 import static edu.illinois.starts.smethods.MethodLevelStaticDepsBuilder.methodName2MethodNames;
 
 /**
  * Utility methods for dealing with the .zlc format.
  */
 public class ZLCHelper implements StartsConstants {
     public static final String zlcFile = "deps.zlc";
     public static final String STAR_FILE = "file:*";
     private static final Logger LOGGER = Logger.getGlobal();
     private static Map<String, ZLCData> zlcDataMap;
     private static final String NOEXISTING_ZLCFILE_FIRST_RUN = "@NoExistingZLCFile. First Run?";

     private static HashMap<String, Set<String>> clModifiedClassesMap = new HashMap<>( );
 
     public ZLCHelper() {
         zlcDataMap = new HashMap<>();
     }
 
 // TODO: Uncomment and fix this method. The problem is that it does not track newly added tests correctly
 //    public static void updateZLCFile(Map<String, Set<String>> testDeps, ClassLoader loader,
 //                                     String artifactsDir, Set<String> changed) {
 //        long start = System.currentTimeMillis();
 //        File file = new File(artifactsDir, zlcFile);
 //        if (! file.exists()) {
 //            Set<ZLCData> zlc = createZLCData(testDeps, loader);
 //            Writer.writeToFile(zlc, zlcFile, artifactsDir);
 //        } else {
 //            Set<ZLCData> zlcData = new HashSet<>();
 //            if (zlcDataMap != null) {
 //                for (ZLCData data : zlcDataMap.values()) {
 //                    String extForm = data.getUrl().toExternalForm();
 //                    if (changed.contains(extForm)) {
 //                         we need to update tests for this zlcData before adding
 //                        String fqn = Writer.toFQN(extForm);
 //                        Set<String> tests = new HashSet<>();
 //                        if (testDeps.keySet().contains(fqn)) {
 //                             a test class changed, it affects on itself
 //                            tests.add(fqn);
 //                        }
 //                        for (String test : testDeps.keySet()) {
 //                            if (testDeps.get(test).contains(fqn)) tests.add(test);
 //                        }
 //                        if (tests.isEmpty()) {
 //                             this dep no longer has ant tests depending on it???
 //                            continue;
 //                        }
 //                        data.setTests(tests);
 //                    }
 //                    zlcData.add(data);
 //                }
 //            }
 //            Writer.writeToFile(zlcData, zlcFile, artifactsDir);
 //        }
 //        long end = System.currentTimeMillis();
 //        System.out.println(TIME_UPDATING_CHECKSUMS + (end - start) + MS);
 //    }
 
     public static void updateZLCFile(Map<String, Set<String>> testDeps, ClassLoader loader,
                                      String artifactsDir, Set<String> unreached, boolean useThirdParty,
                                      ZLCFormat format)   {
         // TODO: Optimize this by only recomputing the checksum+tests for changed classes and newly added tests
         long start = System.currentTimeMillis();
         LOGGER.log(Level.FINE, "ZLC format: " + format.toString());
         ZLCFileContent zlc;
         Boolean methodlevel = false;
         if(methodlevel){
             CreateMZLC(artifactsDir);
             System.out.println("the method");
             testDeps = test2methods;
             zlc = createMZLCData(testDeps, loader, useThirdParty, format);
             
         }else{
             System.out.println("The class");
          zlc = createZLCData(testDeps, loader, useThirdParty, format);
         }

         Writer.writeToFile(zlc, zlcFile, artifactsDir);
         long end = System.currentTimeMillis();
         LOGGER.log(Level.FINE, "[PROFILE] updateForNextRun(updateZLCFile): " + Writer.millsToSeconds(end - start));
     }
    
      public static void updateZLCFileMethod(Map<String, Set<String>> testDeps, ClassLoader loader,
                                      String artifactsDir, Set<String> unreached, boolean useThirdParty,
                                      ZLCFormat format)   {
         // TODO: Optimize this by only recomputing the checksum+tests for changed classes and newly added tests
         long start = System.currentTimeMillis();
         LOGGER.log(Level.FINE, "ZLC format: " + format.toString());
         ZLCFileContent zlc;
         Boolean methodlevel = true;
         if(methodlevel){
             CreateMZLC(artifactsDir);
             System.out.println("the method");
             testDeps = test2methods;
             zlc = createMZLCData(testDeps, loader, useThirdParty, format);
             
         }else{
             System.out.println("The class");
          zlc = createZLCData(testDeps, loader, useThirdParty, format);
         }
       
 

         Writer.writeToFile(zlc, zlcFile, artifactsDir);
         long end = System.currentTimeMillis();
         LOGGER.log(Level.FINE, "[PROFILE] updateForNextRun(updateZLCFile): " + Writer.millsToSeconds(end - start));
     }

     
 
     public static void CreateMZLC(String artifactsDir) {
       // MethodLevelStaticDepsBuilder Mbuilder = new MethodLevelStaticDepsBuilder();
        //String TargetClassesPath = artifactsDir.replace("/.starts/", "/target/");
        //Mbuilder.SmethodFileGen(TargetClassesPath);
        try{
         main(artifactsDir);
        }catch (Exception e) {
         throw new RuntimeException(e);
     }

     writeMapToCSV(methodName2MethodNames, artifactsDir+"/methodgraph.csv");

        
     }
     public static ZLCFileContent createMZLCData(
         Map<String, Set<String>> testDeps,
         ClassLoader loader,
         boolean useJars,
         ZLCFormat format
 ) {
      System.out.println("Creating ZLC data...");
     long start = System.currentTimeMillis();
     List<ZLCData> zlcData = new ArrayList<>();
     Set<String> deps = new HashSet<>();
     ChecksumUtil checksumUtil = new ChecksumUtil(true);
 
 
 
 
     // merge all the deps for all tests into a single set
     for (String test : testDeps.keySet()) {
         deps.addAll(testDeps.get(test));
     }
     ArrayList<String> testList = new ArrayList<>(testDeps.keySet());  // all tests
 

 
     // for each dep, find it's url, checksum and tests that depend on it
     for (String dep : deps) {
         // Here we have the method dependencies after new edit. So we get their classes, then the method again from the InputStream
         // May be we can get the method directly.
         String klas = ChecksumUtil.toClassName(dep.split("#")[0]);
         String methodName = dep.split("#")[1].replace("()", "");
         if (methodName.startsWith("<")) continue;  // skip constructors and initializers
 
 //            System.out.println("OUR METHOD: " + methodName);
 
         // Method url
         // contentprinter for method -> String methodContent
         // checksum for methodContent
         if (Types.isIgnorableInternalName(klas)) {
             continue;
         }
 
         URL url = loader.getResource(klas);
         String path = url.getPath();
 //            System.out.println("URL: " + path);
         ClassNode node = new ClassNode(Opcodes.ASM5);
         ClassReader reader = null;
         try {
             reader = new ClassReader(new FileInputStream(path));
         } catch (IOException e) {
             System.out.println("[ERROR] reading class: " + klas);
             continue;
         }
         String methodChecksum = null;
         reader.accept(node, ClassReader.SKIP_DEBUG);
         List<MethodNode> methods = node.methods;
         for (MethodNode method : methods) {
             // Skip constructors and initializers
 //                System.out.println("METHOD in loop: " + method.name);
             if (!method.name.equals(methodName)) continue;
             //System.out.println(methodName + ":" + method.desc);
             String methodContent = printMethodContent(method);
             try {
                 methodChecksum = ChecksumUtil.computeMethodChecksum(methodContent);
             } catch (IOException e) {
                 throw new RuntimeException(e);
             }
 //                System.out.println("METHOD CHECKSUM! ->   " + methodChecksum);
         }
 
         if (url == null) {
             continue;
         }
         String extForm = url.toExternalForm();
         if (ChecksumUtil.isWellKnownUrl(extForm) || (!useJars && extForm.startsWith("jar:"))) {
             continue;
         }
 //            String checksum = checksumUtil.computeSingleCheckSum(url);
         String checksum = methodChecksum;
         String classURL = url.toString();
         URL newUrl = null;
         try {
             newUrl = new URL(classURL + "#" + methodName);
         } catch (MalformedURLException e) {
             throw new RuntimeException(e);
         }
 //            System.out.println("NEW URL: " + newUrl);
         switch (format) {
             case PLAIN_TEXT:
                 Set<String> testsStr = new HashSet<>();
                 for (String test: testDeps.keySet()) {
                     if (testDeps.get(test).contains(dep)) {
                         testsStr.add(test);
                     }
                 }
                 zlcData.add(new ZLCData(newUrl, checksum, format, testsStr, null));
                 break;
             case INDEXED:
                 Set<Integer> testsIdx = new HashSet<>();
                 for (int i = 0; i < testList.size(); i++) {
                     if (testDeps.get(testList.get(i)).contains(dep)) {
                         testsIdx.add(i);
                     }
                 }
                 zlcData.add(new ZLCData(newUrl, checksum, format, null, testsIdx));
                 break;
             default:
                 throw new RuntimeException("Unexpected ZLCFormat");
         }
     }
     long end = System.currentTimeMillis();
     LOGGER.log(Level.FINEST, "[TIME]CREATING ZLC FILE: " + (end - start) + MILLISECOND);
 //        System.out.println("ZLC DATA");
 //        System.out.println(zlcData);
 //        System.out.println("testList AFTER");
 //        System.out.println(testList);
     return new ZLCFileContent(testList, zlcData, format);
 }
     public static ZLCFileContent createZLCData(
             Map<String, Set<String>> testDeps,
             ClassLoader loader,
             boolean useJars,
             ZLCFormat format
     ) {
  
 
         long start = System.currentTimeMillis();
         List<ZLCData> zlcData = new ArrayList<>();
         Set<String> deps = new HashSet<>();
         ChecksumUtil checksumUtil = new ChecksumUtil(true);
         // merge all the deps for all tests into a single set
         for (String test : testDeps.keySet()) {
             deps.addAll(testDeps.get(test));
         }
         ArrayList<String> testList = new ArrayList<>(testDeps.keySet());  // all tests
 
 
         // for each dep, find it's url, checksum and tests that depend on it
         for (String dep : deps) {
             String klas = ChecksumUtil.toClassName(dep);
             if (Types.isIgnorableInternalName(klas)) {
                 continue;
             }
             URL url = loader.getResource(klas);
             if (url == null) {
                 continue;
             }
             String extForm = url.toExternalForm();
             if (ChecksumUtil.isWellKnownUrl(extForm) || (!useJars && extForm.startsWith("jar:"))) {
                 continue;
             }
             String checksum = checksumUtil.computeSingleCheckSum(url);
             switch (format) {
                 case PLAIN_TEXT:
                     Set<String> testsStr = new HashSet<>();
                     for (String test: testDeps.keySet()) {
                         if (testDeps.get(test).contains(dep)) {
                             testsStr.add(test);
                         }
                     }
                     zlcData.add(new ZLCData(url, checksum, format, testsStr, null));
                     break;
                 case INDEXED:
                     Set<Integer> testsIdx = new HashSet<>();
                     for (int i = 0; i < testList.size(); i++) {
                         if (testDeps.get(testList.get(i)).contains(dep)) {
                             testsIdx.add(i);
                         }
                     }
                     zlcData.add(new ZLCData(url, checksum, format, null, testsIdx));
                     break;
                 default:
                     throw new RuntimeException("Unexpected ZLCFormat");
             }
         }
         long end = System.currentTimeMillis();
         LOGGER.log(Level.FINEST, "[TIME]CREATING ZLC FILE: " + (end - start) + MILLISECOND);
         return new ZLCFileContent(testList, zlcData, format);
     }
     public static Pair<Set<String>, Set<String>> getMChangedData(String artifactsDir, boolean cleanBytes) {

        
        try {
            main(artifactsDir);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Map<String, Set<String>> methodgraph = methodName2MethodNames;

        Map<String, Set<String>> newMethodgraph = new HashMap<>();
for (String key : methodgraph.keySet()) {
    String newKey = key.replace("()", "");
    Set<String> value = methodgraph.get(key);
    newMethodgraph.put(newKey, value);
}
methodgraph = newMethodgraph;

        long start = System.currentTimeMillis();
        File zlc = new File(artifactsDir, zlcFile);
        if (!zlc.exists()) {
            LOGGER.log(Level.FINEST, NOEXISTING_ZLCFILE_FIRST_RUN);
            return null;
        }
        //System.out.println("WE ARE HERE");

        Set<String> changedClasses = new HashSet<>();
        Set<String> nonAffected = new HashSet<>();
        Set<String> affected = new HashSet<>();
        Set<String> starTests = new HashSet<>();
        ChecksumUtil checksumUtil = new ChecksumUtil(cleanBytes);
        try {
            List<String> zlcLines = Files.readAllLines(zlc.toPath(), Charset.defaultCharset());
            String firstLine = zlcLines.get(0);
            String space = WHITE_SPACE;

            // check whether the first line is for *
            if (firstLine.startsWith(STAR_FILE)) {
                String[] parts = firstLine.split(space);
                starTests = fromCSV(parts[2]);
                zlcLines.remove(0);
            }

            ZLCFormat format = ZLCFormat.PLAIN_TEXT;  // default to plain text
            if (zlcLines.get(0).equals(ZLCFormat.PLAIN_TEXT.toString())) {
                format = ZLCFormat.PLAIN_TEXT;
                zlcLines.remove(0);
            } else if (zlcLines.get(0).equals(ZLCFormat.INDEXED.toString())) {
                format = ZLCFormat.INDEXED;
                zlcLines.remove(0);
            }

            int testsCount = -1;  //on PLAIN_TEXT, testsCount+1 will starts from 0
            ArrayList<String> testsList = null;
            if (format == ZLCFormat.INDEXED) {
                try {
                    testsCount = Integer.parseInt(zlcLines.get(0));
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                }
                testsList = new ArrayList<>(zlcLines.subList(1, testsCount + 1));
            }
            for (int i = testsCount + 1; i < zlcLines.size(); i++) {
//                if (i < 5) {
//                    System.out.println(zlcLines.get(i));
//                }
                String line = zlcLines.get(i);
                //System.out.println("line = "+ line);
                String[] parts = line.split(space);
                // classURL#methodname
                String stringURL = parts[0];
                String classURL = stringURL.split("#")[0];
                String methodName = stringURL.split("#")[1];

                String oldCheckSum = parts[1];

        
                Set<String> tests;
                if (format == ZLCFormat.INDEXED) {
                    Set<Integer> testsIdx = parts.length == 3 ? fromCSVToInt(parts[2]) : new HashSet<>();
                    tests = testsIdx.stream().map(testsList::get).collect(Collectors.toSet());
                } else {
                    tests = parts.length == 3 ? fromCSV(parts[2]) : new HashSet<>();
                }
                nonAffected.addAll(tests);
//               URL url = new URL(stringURL);
                URL url = new URL(classURL);


//               URL url = loader.getResource(klas);
                String path = url.getPath();
                ClassNode node = new ClassNode(Opcodes.ASM5);
                ClassReader reader = null;
                try {
                    reader = new ClassReader(new FileInputStream(path));
                } catch (IOException e) {
                    throw new IOException(e);
                }

                String newMethodChecksum = null;
                reader.accept(node, ClassReader.SKIP_DEBUG);
                List<MethodNode> methods = node.methods;
                for (MethodNode method : methods) {
                    // Skip constructors and initializers
                    if (!method.name.equals(methodName)) continue;
                    //System.out.println(method.name + ":" + method.desc);
                    String methodContent = printMethodContent(method);
                    try {
                        newMethodChecksum = ChecksumUtil.computeMethodChecksum(methodContent);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                  
                }

//                String newCheckSum = checksumUtil.computeSingleCheckSum(url);
                String newCheckSum = newMethodChecksum;
                if(newCheckSum==null && oldCheckSum.equals("null")){
                    continue;
                }
                if (!newCheckSum.equals(oldCheckSum)) {
                    affected.addAll(tests);
                    String keyname = stringURL.substring(stringURL.indexOf("org"));
                    keyname =keyname.replace(".class", "");
                    Set<String> allofthemethod = dfsMethodGraph(keyname,methodgraph);
                    changedClasses.addAll(allofthemethod);

                }
                if (newCheckSum.equals("-1")) {
                    // a class was deleted or auto-generated, no need to track it in zlc
                    LOGGER.log(Level.FINEST, "Ignoring: " + url);
                    continue;
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        if (!changedClasses.isEmpty()) {
            // there was some change so we need to add all tests that reach star, if any
            affected.addAll(starTests);
        }
        nonAffected.removeAll(affected);
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, TIME_COMPUTING_NON_AFFECTED + (end - start) + MILLISECOND);
        //System.out.println("Non affected: " + nonAffected);
        System.out.println("Affected methods: "+ changedClasses);
        System.out.println("Affected tests: " + affected);
        System.out.println("Non affected tests: " + nonAffected);
        // TODO: change all '/' in the nonAffected to '.'
        /*for (String s : nonAffected) {
            System.out.println(s.replace('/', '.'));
        }*/
        return new Pair<>(nonAffected, changedClasses);
    }

 
     public static Pair<Set<String>, Set<String>> getChangedData(String artifactsDir, boolean cleanBytes) {
         long start = System.currentTimeMillis();
         File zlc = new File(artifactsDir, zlcFile);
 
         if (!zlc.exists()) {
            
             
             LOGGER.log(Level.FINEST, NOEXISTING_ZLCFILE_FIRST_RUN);
             return null;
         }
         Set<String> changedClasses = new HashSet<>();
         Set<String> nonAffected = new HashSet<>();
         Set<String> affected = new HashSet<>();
         Set<String> starTests = new HashSet<>();
         ChecksumUtil checksumUtil = new ChecksumUtil(cleanBytes);
         try {
             List<String> zlcLines = Files.readAllLines(zlc.toPath(), Charset.defaultCharset());
             String firstLine = zlcLines.get(0);//class t affected tests
             String space = WHITE_SPACE;
             
 
             // check whether the first line is for *
             if (firstLine.startsWith(STAR_FILE)) {
                 String[] parts = firstLine.split(space);
                 starTests = fromCSV(parts[2]);
                 zlcLines.remove(0);
             }
 
             ZLCFormat format = ZLCFormat.PLAIN_TEXT;  // default to plain text
             if (zlcLines.get(0).equals(ZLCFormat.PLAIN_TEXT.toString())) {
                 format = ZLCFormat.PLAIN_TEXT;
                 zlcLines.remove(0);
             } else if (zlcLines.get(0).equals(ZLCFormat.INDEXED.toString())) {
                 format = ZLCFormat.INDEXED;
                 zlcLines.remove(0);
             }
 
             int testsCount = -1;  // on PLAIN_TEXT, testsCount+1 will starts from 0
             ArrayList<String> testsList = null;
             if (format == ZLCFormat.INDEXED) {
                 try {
                     testsCount = Integer.parseInt(zlcLines.get(0));
                 } catch (NumberFormatException nfe) {
                     nfe.printStackTrace();
                 }
                 testsList = new ArrayList<>(zlcLines.subList(1, testsCount + 1));
             }
 
             for (int i = testsCount + 1; i < zlcLines.size(); i++) {
                 String line = zlcLines.get(i);
                 String[] parts = line.split(space);
                 String stringURL = parts[0];
                 String oldCheckSum = parts[1];
                 Set<String> tests;
                 if (format == ZLCFormat.INDEXED) {
                     Set<Integer> testsIdx = parts.length == 3 ? fromCSVToInt(parts[2]) : new HashSet<>();
                     tests = testsIdx.stream().map(testsList::get).collect(Collectors.toSet());
                 } else {
                     tests = parts.length == 3 ? fromCSV(parts[2]) : new HashSet<>();
                 }
                 nonAffected.addAll(tests);
                 URL url = new URL(stringURL);
                 String newCheckSum = checksumUtil.computeSingleCheckSum(url);
                 if (!newCheckSum.equals(oldCheckSum)) {
                     
                     affected.addAll(tests);
                     changedClasses.add(stringURL);
                 }
                 if (newCheckSum.equals("-1")) {
                     // a class was deleted or auto-generated, no need to track it in zlc
                     LOGGER.log(Level.FINEST, "Ignoring: " + url);
                     continue;
                 }
             }
         } catch (IOException ioe) {
             ioe.printStackTrace();
         }
         if (!changedClasses.isEmpty()) {
             // there was some change so we need to add all tests that reach star, if any
             affected.addAll(starTests);
         }
         nonAffected.removeAll(affected);
 
        /*  System.out.println("The affected:");
         System.out.println(affected);
         System.out.println("The unaffected:");
         System.out.println(nonAffected);
         */
         long end = System.currentTimeMillis();
         LOGGER.log(Level.FINEST, TIME_COMPUTING_NON_AFFECTED + (end - start) + MILLISECOND);
       
         return new Pair<>(nonAffected, changedClasses);
     }
 
     private static Set<String> fromCSV(String tests) {
         return new HashSet<>(Arrays.asList(tests.split(COMMA)));
     }
 
     private static Set<Integer> fromCSVToInt(String tests) {
         return Arrays.stream(tests.split(COMMA)).map(Integer::parseInt).collect(Collectors.toSet());
     }
 
     public static Set<String> getExistingClasses(String artifactsDir) {
         
         Set<String> existingClasses = new HashSet<>();
         long start = System.currentTimeMillis();
         File zlc = new File(artifactsDir, zlcFile);
         if (!zlc.exists()) {
             LOGGER.log(Level.FINEST, NOEXISTING_ZLCFILE_FIRST_RUN);
             return existingClasses;
         }
         try {
             List<String> zlcLines = Files.readAllLines(zlc.toPath(), Charset.defaultCharset());
             for (String line : zlcLines) {
                 if (line.startsWith("file")) {
                     existingClasses.add(Writer.urlToFQN(line.split(WHITE_SPACE)[0]));
                 }
             }
         } catch (IOException ioe) {
             ioe.printStackTrace();
         }
         long end = System.currentTimeMillis();
         LOGGER.log(Level.FINEST, "[TIME]COMPUTING EXISTING CLASSES: " + (end - start) + MILLISECOND);
         return existingClasses;
     }
 
     public static Set<String> getAffectedTests(Set<String> changedMethods, Map<String, Set<String>> methodName2MethodNames, Set<String> testClasses){
         Set<String> affectedTests = new HashSet<>();
         // BFS, starting with the changed methods
         ArrayDeque<String> queue = new ArrayDeque<>();
         queue.addAll(changedMethods);
         Set<String> visitedMethods = new TreeSet<>();
         while (!queue.isEmpty()){
             String currentMethod = queue.pollFirst();
             String currentClass = currentMethod.split("#|\\$")[0];
             if (testClasses.contains(currentClass)){
                 affectedTests.add(currentClass);
             }
             for (String invokedMethod : methodName2MethodNames.getOrDefault(currentMethod, new HashSet<>())){
                 if (!visitedMethods.contains(invokedMethod)) {
                     queue.add(invokedMethod);
                     visitedMethods.add(invokedMethod);
                 }
             }
         }
         return affectedTests;
     }


   

  

    public static String printMethodContent(MethodNode node) {
        Printer printer = new Textifier(Opcodes.ASM5) {
            @Override
            public void visitLineNumber(int line, Label start) {
            }
        };
        TraceMethodVisitor methodPrinter = new TraceMethodVisitor(printer);
        node.accept(methodPrinter);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        // include the access code in case of access code changes
        String methodContent = node.access + "\n"+ node.signature+"\n"+ sw.toString();
//        LOGGER.debug("Method " + node.name + " content: " + methodContent);
        return methodContent;
    }

    private static void writeMapToCSV(Map<String, Set<String>> methodName2MethodNames, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("Method Name,Method Names\n"); // header row
            
            for (String methodName : methodName2MethodNames.keySet()) {
                StringBuilder methodNamesString = new StringBuilder();
                Set<String> methodNames = methodName2MethodNames.get(methodName);
                for (String method : methodNames) {
                    methodNamesString.append(method).append(";"); // separate method names with semicolon
                }
                writer.write(methodName + "," + methodNamesString.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Set<String> dfsMethodGraph(String startMethod, Map<String, Set<String>> methodGraph) {
        Set<String> visited = new HashSet<>();
        Stack<String> stack = new Stack<>();
        stack.push(startMethod);
        while (!stack.isEmpty()) {
            String currentMethod = stack.pop();
            if (!visited.contains(currentMethod)) {
                visited.add(currentMethod);
                Set<String> relatedMethods = methodGraph.getOrDefault(currentMethod, new HashSet<>());
                for (String relatedMethod : relatedMethods) {
                    stack.push(relatedMethod);
                }
            }
        }
        return visited;
    }

    }
