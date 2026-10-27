package matybachy.argentumforge.client.gui.video;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/**
 * "ArgentumForge..." button added to OptiFine's video settings screen (see {@code ClientProxy.onInitGui}). It opens
 * the ArgentumForge options without the vanilla video options, which OptiFine's screen already shows. The click is
 * handled here instead of in the screen's actionPerformed, so it does not depend on how OptiFine treats unknown
 * button ids; returning false keeps the screen from handling the click as well.
 */
public final class OptiFineMenuButton extends GuiButton {
    /** Far outside the ids vanilla and OptiFine use on that screen. */
    public static final int ID = 7_100_001;

    private final GuiScreen owner;

    public OptiFineMenuButton(GuiScreen owner) {
        super(ID, 5, 5, 100, 20, I18n.format("argentumforge.video.optifineButton"));
        this.owner = owner;
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!super.mousePressed(mc, mouseX, mouseY)) return false;
        playPressSound(mc.getSoundHandler());
        mc.displayGuiScreen(ArgentumVideoSettingsGui.optionsOnly(owner));
        return false;
    }
}
