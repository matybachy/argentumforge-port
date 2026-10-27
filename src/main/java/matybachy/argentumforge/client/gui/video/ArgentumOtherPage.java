package matybachy.argentumforge.client.gui.video;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.client.compat.CompatibilityEnvironment;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

/** Detected clients, compatibility switches and a reset of every ArgentumForge option. */
final class ArgentumOtherPage extends ArgentumSettingsScreen {
    private static final int STATUS_ID = 300;
    private static final int RESET_ID = 310;

    ArgentumOtherPage(GuiScreen parent) {
        super(parent, "argentumforge.video.title.other");
    }

    @Override
    protected void addControls() {
        addStatus(STATUS_ID, status("OptiFine", CompatibilityEnvironment.isOptiFinePresent()));
        addStatus(STATUS_ID + 1, status("FeatherOpt", CompatibilityEnvironment.isFeatherOptPresent()));
        addStatus(STATUS_ID + 2, status("Dawn", CompatibilityEnvironment.isDawnPresent()));
        endRow();
        addConfigOption(ConfigOption.of("disableExperimentalRendererWithFeatherOpt"));
        addConfigOption(ConfigOption.overlayOnly("disableOptionalEffectsWithFeatherOpt"));
        addConfigOption(ConfigOption.of("disableExperimentalRendererWithDawn"));
        addWideButton(RESET_ID, "argentumforge.video.reset");
    }

    private static String status(String name, boolean detected) {
        return I18n.format(detected ? "argentumforge.video.detected" : "argentumforge.video.notDetected", name);
    }

    @Override
    protected void onPageButton(GuiButton button) {
        if (button.id == RESET_ID) {
            openChild(new GuiYesNo(this, I18n.format("argentumforge.video.reset.question"),
                    I18n.format("argentumforge.video.reset.detail"), RESET_ID) {
                // Vanilla Esc would close every screen and skip applying pending options; treat it as "No".
                @Override
                protected void keyTyped(char typedChar, int keyCode) {
                    if (keyCode == Keyboard.KEY_ESCAPE) ArgentumOtherPage.this.confirmClicked(false, RESET_ID);
                }
            });
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (result && id == RESET_ID) {
            ArgentumConfig.resetToDefaults();
            // The master switch and terrainMode may change: applied when the video settings are left.
            VideoSettingsSession.onChanged(true);
        }
        // Back from the confirmation screen; initGui rebuilds the buttons from the config.
        mc.displayGuiScreen(this);
    }
}
