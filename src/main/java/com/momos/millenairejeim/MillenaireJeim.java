package com.momos.millenairejeim;

import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import com.momos.millenairejeim.util.MMLog;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MillenaireJeim.MODID)
public class MillenaireJeim {

    public static final String MODID = "millenairejeim";

    /**[Readme]
     * 在{@link com.momos.millenairejeim.jei.Trade.MillTradeCategory#draw}中我的实现方式会导致每次查阅对象会产生一些额外的性能消耗(没有让recipe缓存String)，这么做可以在切换语言后依旧正常运行，但我不确定
     */
    public MillenaireJeim(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::onClientSetup);

            // [新注释] 将离开世界的客户端网络事件注册到 NeoForge 主事件总线 (NeoForge.EVENT_BUS)
            NeoForge.EVENT_BUS.addListener(this::onClientLoggingOut);
        }
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 安全抓取客户端当前语言代码（例如 "zh_cn", "en_us", "fr_fr"）
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

    /**
     * [新注释] 监听玩家从服务器/单人世界登出/离开事件。
     * 在离开世界时主动清空 {@link MillenaireAPIHelper} 的静态映射缓存，防止跨世界切换或重载时旧数据残留。
     *
     * @param event {@link ClientPlayerNetworkEvent.LoggingOut} 客户端网络登出事件
     */
    private void onClientLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        MillenaireAPIHelper.clearCache();
        MMLog.info(
                "[Millenaire-JEI] 已离开世界，成功清空 MillenaireAPIHelper 静态缓存。",
                "[Millenaire-JEI] Left world, cleared MillenaireAPIHelper static cache.",
                "[Millenaire-JEI] Monde quitté, cache statique MillenaireAPIHelper effacé."
        );
    }
}