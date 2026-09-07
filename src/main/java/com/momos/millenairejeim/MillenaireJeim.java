package com.momos.millenairejeim;

import com.momos.millenairejeim.util.MMLog;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(MillenaireJeim.MODID)
public class MillenaireJeim {

    public static final String MODID = "millenairejeim";

    /**[Readme]
     * 在{@link com.momos.millenairejeim.jei.Trade.MillTradeCategory#draw}中我的实现方式会导致每次查阅对象会产生一些额外的性能消耗(没有让recipe缓存String)，这么做可以在切换语言后依旧正常运行，但我不确定
     */
    public MillenaireJeim(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::onClientSetup);
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
}