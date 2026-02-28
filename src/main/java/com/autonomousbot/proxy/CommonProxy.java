package com.autonomousbot.proxy;

/**
 * Серверный прокси — вызывается на выделенном сервере и на клиенте.
 * Не содержит клиентского кода (рендеринг и т.д.).
 */
public class CommonProxy {
    public void registerRenderers() {
        // На сервере рендеринг не нужен
    }
}
