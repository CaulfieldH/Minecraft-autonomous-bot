package com.autonomousbot.ai;

/**
 * Режимы работы автономного бота.
 * Переключается через конфиг или команду /bot mode <id> <режим>
 */
public enum BotMode {

    RESOURCE_GATHERING("resource_gathering", "Gathering Resources"),
    BUILDING          ("building",           "Building Shelter"),
    PVP               ("pvp",                "PvP Combat");

    private final String configName;
    private final String displayName;

    BotMode(String configName, String displayName) {
        this.configName  = configName;
        this.displayName = displayName;
    }

    public String getConfigName()  { return configName;  }
    public String getDisplayName() { return displayName; }

    public static BotMode fromString(String s) {
        if (s == null) return RESOURCE_GATHERING;
        for (BotMode mode : values()) {
            if (mode.configName.equalsIgnoreCase(s.trim())) return mode;
        }
        return RESOURCE_GATHERING;
    }
}
