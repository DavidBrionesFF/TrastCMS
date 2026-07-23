package com.nattechnologies.trastcms.example;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public final class HelloPlugin extends Plugin {
    public HelloPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        getWrapper().getPluginManager().getSystemVersion();
    }
}
