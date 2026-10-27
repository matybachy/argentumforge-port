package matybachy.argentumforge.client.gui;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.ArgentumForge;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game settings screen (Mods list -> ArgentumForge -> Config, or the "Open ArgentumForge settings" key).
 * Changes apply immediately: Forge posts {@code OnConfigChangedEvent} on Done, and {@code ClientProxy} saves the
 * file and calls {@link ArgentumConfig#sync()}. The integrated terrain path installs or restores itself on the
 * next client tick.
 */
public final class ArgentumConfigScreen extends GuiConfig {
    public ArgentumConfigScreen(GuiScreen parent) {
        super(parent, elements(), ArgentumForge.MODID, false, false, "ArgentumForge",
                GuiConfig.getAbridgedConfigPath(ArgentumConfig.getConfiguration().toString()));
    }

    private static List<IConfigElement> elements() {
        Configuration cfg = ArgentumConfig.getConfiguration();
        List<IConfigElement> list = new ArrayList<IConfigElement>();
        if (cfg == null) return list;
        list.add(new ConfigElement(cfg.getCategory(ArgentumConfig.CAT_RENDERER)));
        list.add(new ConfigElement(cfg.getCategory(ArgentumConfig.CAT_FEATURES)));
        list.add(new ConfigElement(cfg.getCategory(ArgentumConfig.CAT_FALLBACKS)));
        return list;
    }
}
