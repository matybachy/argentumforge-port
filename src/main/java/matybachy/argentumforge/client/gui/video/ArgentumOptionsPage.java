package matybachy.argentumforge.client.gui.video;

import net.minecraft.client.gui.GuiScreen;

/** A subpage that lists config options in the two-column grid. */
class ArgentumOptionsPage extends ArgentumSettingsScreen {
    private final ConfigOption[] options;

    ArgentumOptionsPage(GuiScreen parent, String titleKey, ConfigOption... options) {
        super(parent, titleKey);
        this.options = options;
    }

    @Override
    protected void addControls() {
        for (ConfigOption option : options) addConfigOption(option);
    }
}
