package com.sham1316;

import com.google.gson.Gson;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Puts bamboo build-time information into property files.
 */
@Mojo(name = "info", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class InfoMojo extends AbstractMojo {

    /**
     * The format to save properties in: {@code 'properties'} or {@code 'json'}.
     */
    @Parameter(defaultValue = "properties")
    String format;
    /**
     * <p>The location of {@code 'bamboo.properties'} file.
     *
     * <p>The path here is relative to your project src directory.</p>
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/bamboo.properties")
    String bambooPropertiesFileName;

    public void execute() throws MojoExecutionException {
        getLog().info("Write env to file " + bambooPropertiesFileName + ". Format: " + format + ".");
        Path parent = Paths.get(bambooPropertiesFileName).getParent();
        if (!Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                //fail to create directory
                throw new RuntimeException("Cannot create dir: " + parent, e);
            }
        }

        Map<String, String> env = System.getenv();
        Map<String, String> bamboo_env = env.entrySet().stream()
                .filter(x -> x.getKey().startsWith("bamboo_"))
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
        if ("properties".equals(format)) {
            Properties prop = new Properties();
            prop.putAll(bamboo_env);
            try (OutputStream outputStream = new FileOutputStream(bambooPropertiesFileName)) {
                prop.store(outputStream, null);
            } catch (IOException e) {
                throw new RuntimeException("Cannot create bamboo env file: " + bambooPropertiesFileName, e);
            }
        } else if ("json".equals(format)) {
            try (Writer writer = new FileWriter(bambooPropertiesFileName)) {
                Gson gson = new Gson();
                gson.toJson(bamboo_env, writer);
            } catch (IOException e) {
                throw new RuntimeException("Cannot create bamboo env file: " + bambooPropertiesFileName, e);
            }
        } else {
            throw new RuntimeException("Unknown format bamboo env file: " + format);
        }
    }
}
