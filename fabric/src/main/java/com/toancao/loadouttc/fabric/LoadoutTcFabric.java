package com.toancao.loadouttc.fabric;

import com.toancao.loadouttc.LoadoutTcClient;
import com.toancao.loadouttc.platform.PlatformHolder;
import net.fabricmc.api.ClientModInitializer;

/** Fabric entrypoint: install the platform, then hand over to the shared bootstrap. */
public final class LoadoutTcFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PlatformHolder.install(new FabricPlatform());
        LoadoutTcClient.init();
    }
}
