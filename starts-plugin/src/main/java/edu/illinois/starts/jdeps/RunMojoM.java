package edu.illinois.starts.jdeps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.maven.AgentLoader;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;



@Mojo(name = "runm", requiresDependencyResolution = ResolutionScope.TEST)
public class RunMojoM extends RunMojo{
   public void execute() throws MojoExecutionException {
       super.executeM();
   } 
}
