package com.autonomousbot.proxy;

import com.autonomousbot.entity.EntityAutonomousBot;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;

/**
 * Клиентский прокси — выполняется только на клиенте.
 * Регистрирует рендер сущности бота (двуногая модель, как у игрока).
 */
public class ClientProxy extends CommonProxy {

    @Override
    public void registerRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(
            EntityAutonomousBot.class,
            new RenderBiped(new ModelBiped(), 0.5F)
        );
    }
}
