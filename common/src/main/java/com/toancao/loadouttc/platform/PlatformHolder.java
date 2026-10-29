package com.toancao.loadouttc.platform;

/** Holds the single {@link Platform} instance, installed by the active loader module. */
public final class PlatformHolder {

    private static Platform platform;

    private PlatformHolder() {
    }

    public static void install(Platform implementation) {
        if (platform != null) {
            throw new IllegalStateException("Platform already installed: " + platform.name());
        }
        platform = implementation;
    }

    public static Platform get() {
        Platform current = platform;
        if (current == null) {
            throw new IllegalStateException("Platform not installed - is the mod entrypoint missing?");
        }
        return current;
    }
}
