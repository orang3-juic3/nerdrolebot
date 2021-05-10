package me.alex.meta;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.BurstFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;


@Plugin(
        name = "TestAppender",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE)
public class TestAppender extends AbstractAppender {


    protected TestAppender() {
        super("TestAppender", BurstFilter.newBuilder().build(), PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY);
    }

    @PluginFactory
    public static TestAppender createAppender() {
        return new TestAppender();
    }

    @Override
    public void append(LogEvent event) {
        System.out.println(event.toImmutable().getMessage());
    }
}