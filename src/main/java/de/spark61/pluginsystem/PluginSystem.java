package de.spark61.pluginsystem;

import io.github.classgraph.AnnotationParameterValueList;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Optional;

/**
 * @author Spark61
 */
public class PluginSystem {
    private static final String PLUGIN_FOLDER = "plugins/";
    private final ArrayList<Plugin> plugins = new ArrayList<>();

    public PluginSystem() throws IOException, IllegalClassFormatException {
        this(true);
    }

    public PluginSystem(final boolean shutdownHook) throws IOException, IllegalClassFormatException {
        this.loadPlugins();

        if (shutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::disablePlugins));
        }
    }


    private void loadPlugin(final @NotNull File jarFile) throws IllegalClassFormatException {
        final ScanResult scanResult = new ClassGraph()
                .enableAnnotationInfo()
                .overrideClasspath(jarFile.getAbsolutePath())
                .scan();


        final String annotation = Plugin.class.getName();

        final Optional<ClassInfo> optionalClassInfo = scanResult.getAllClasses()
                .stream()
                .filter(classInfo -> classInfo.hasAnnotation(annotation))
                .findFirst();

        if (optionalClassInfo.isEmpty()) {
            throw new IllegalClassFormatException("Keine Plugin Annotation in den Plugin gefunden: " + jarFile.getName());
        }

        final ClassInfo classInfo = optionalClassInfo.get();

        if (classInfo.getAnnotationInfo().stream().noneMatch(classInfo1 -> classInfo1.getName().equals(annotation))) {
            throw new IllegalClassFormatException("Keine Plugin Annotation in den Plugin gefunden: " + jarFile.getName());
        }

        final AnnotationParameterValueList parameter = classInfo.getAnnotationInfo(annotation).getParameterValues();
        final String pluginName = (String) parameter.getValue("pluginName");
        try {

            final Plugin plugin = (Plugin) Class.forName(classInfo.getName(), true, new URLClassLoader(new URL[]{jarFile.toURI().toURL()}))
                    .getDeclaredConstructor()
                    .newInstance();

            System.out.println("Lade Plugin " + pluginName + "...");

            this.getPlugins().add(plugin);
            plugin.onEnable();

            System.out.println("Das Plugin " + pluginName + " wurde geladen");
        } catch (final MalformedURLException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException exception) {
            System.out.println("Ich konnte das Plugin " + pluginName + " nicht laden");
            exception.printStackTrace();
        }
    }

    public void disablePlugins() {
        this.plugins.forEach(Plugin::onDisable);
        this.plugins.clear();
    }

    public void loadPlugins() throws IOException, IllegalClassFormatException {
        final File folder = new File(PluginSystem.PLUGIN_FOLDER);
        if (!folder.exists()) {
            final boolean created = folder.createNewFile();
        }

        final File[] files = folder.listFiles();

        assert files != null;
        for (final File file : files) {
            if (!file.isDirectory()) {
                this.loadPlugin(file);
            }
        }
    }

    public ArrayList<Plugin> getPlugins() {
        return this.plugins;
    }
}
