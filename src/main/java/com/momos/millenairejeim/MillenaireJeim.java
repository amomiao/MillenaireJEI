package com.momos.millenairejeim;

import com.momos.millenairejeim.api.Forge1201PlatformBridge;
import com.momos.millenairejeim.api.PlatformBridge;
import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import com.momos.millenairejeim.util.MMLog;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(MillenaireJeim.MODID)
public class MillenaireJeim {

    public static final String MODID = "millenairejeim";

    public MillenaireJeim() {
        PlatformBridge.setInstance(new Forge1201PlatformBridge());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
            modEventBus.addListener(this::onClientSetup);

            MinecraftForge.EVENT_BUS.addListener(this::onClientLoggingOut);
        }
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            String clientLang = Minecraft.getInstance().options.languageCode;
            MMLog.setLanguageFromCode(clientLang);
            MMLog.info(
                    "[Millenaire-JEI] 已根据客户端语言设置日志语言为: {}",
                    "[Millenaire-JEI] Log language updated based on client settings: {}",
                    "[Millenaire-JEI] Langue des journaux mise à jour selon le client: {}",
                    MMLog.getCurrentLanguage()
            );
        });
    }

    private void onClientLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        MillenaireAPIHelper.clearCache();
        MMLog.info(
                "[Millenaire-JEI] 已离开世界，成功清空 MillenaireAPIHelper 静态缓存。",
                "[Millenaire-JEI] Left world, cleared MillenaireAPIHelper static cache.",
                "[Millenaire-JEI] Monde quitté, cache statique MillenaireAPIHelper effacé."
        );
    }
}
