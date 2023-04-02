/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

 package edu.illinois.starts.jdeps;

 import java.util.logging.Level;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.logging.Level;
 import org.apache.maven.plugins.annotations.Parameter;
 import edu.illinois.starts.constants.StartsConstants;
 import edu.illinois.starts.helpers.Writer;
 import edu.illinois.starts.util.Logger;
 import edu.illinois.starts.util.Pair;
 import edu.illinois.starts.data.ZLCFormat;

 import org.apache.maven.plugin.MojoExecutionException;
 import org.apache.maven.plugins.annotations.Execute;
 import org.apache.maven.plugins.annotations.LifecyclePhase;
 import org.apache.maven.plugins.annotations.Mojo;
 import org.apache.maven.plugins.annotations.ResolutionScope;
 
 /**
  * Invoked after after running selected tests (see lifecycle.xml for details).
  */
 @Mojo(name = "methoddiff", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
 @Execute(phase = LifecyclePhase.TEST, lifecycle = "starts")
 public class MethodDiffMojo extends DiffMojo implements StartsConstants {
     private Logger logger;
     /**
      * Set this to "false" to disable smart hashing, i.e., to *not* strip
      * Bytecode files of debug info prior to computing checksums. See the "Smart
      * Checksums" Sections in the Ekstazi paper:
      * http://dl.acm.org/citation.cfm?id=2771784
      */
      @Parameter(property = "cleanBytes", defaultValue = TRUE)
      protected boolean cleanBytes;
  
      /**
       * Format of the ZLC dependency file deps.zlc
       * Set to "INDEXED" to store indices of tests
       * Set to "PLAIN_TEXT" to store full URLs of tests
       */
      @Parameter(property = "zlcFormat", defaultValue = "PLAIN_TEXT")
      protected ZLCFormat zlcFormat;
  
      /**
       * Set this to "true" to update test dependencies on disk. The default value of "false"
       * is useful for "dry runs" where one may want to see the diff without updating
       * the test dependencies.
       */
      @Parameter(property = "updateDiffChecksums", defaultValue = FALSE)
      private boolean updateDiffChecksums;
 
     public void execute() throws MojoExecutionException {
        
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
 
        Set<String> changed = new HashSet<>();
        Set<String> nonAffected = new HashSet<>();
        Pair<Set<String>, Set<String>> data = computeMethodChangeData(false);
        String extraText = EMPTY;
        if (data != null) {
            nonAffected = data.getKey();
            changed = data.getValue();
        } else {
            extraText = " (no RTS artifacts; likely the first run)";
        }
        printResult(changed, "ChangedClasses" + extraText);
        if (updateDiffChecksums) {
            updateForNextRunMethod(nonAffected);
        }
     }
 }
 



 