package de.spark61.pluginsystem;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * @author Spark61
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface PluginInfo {
    @NotNull String pluginName();
}
