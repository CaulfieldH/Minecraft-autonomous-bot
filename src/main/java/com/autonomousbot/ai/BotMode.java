package com.autonomousbot.ai;

/**
 * Перечисление режимов работы автономного бота.
 * Переключается через конфиг-файл или команду /bot mode <id> <режим>
 */
public enum BotMode {

    /** Автономный сбор ресурсов: дерево, руда, камень */
    RESOURCE_GATHERING("resource_gathering", "Gathering Resources"),

    /** Строительство убежища из доступных материалов */
    BUILDING("building", "Building Shelter"),

    /** PvP: поиск и атака ближайшего игрока */
    PVP("pvp", "PvP Combat");

    private final String configName;
    private final String displayName;

    BotMode(String configName, String displayName) {
        this.configName  = configName;
        this.displayName = displayName;
    }

    public String getConfigName()  { return configName;  }
    public String getDisplayName() { return displayName; }

    /**
     * Восстановить режим из строки (из конфига или команды).
     * Нечувствительно к регистру. Если строка неизвестна — возвращает RESOURCE_GATHERING.
     */
    public static BotMode fromString(String s) {
        if (s == null) return RESOURCE_GATHERING;
        for (BotMode mode : values()) {
            if (mode.configName.equalsIgnoreCase(s.trim())) {
                return mode;
            }
        }
        return RESOURCE_GATHERING;
    }
}
