package matybachy.argentumforge.client.gui.video;

import matybachy.argentumforge.ArgentumConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiOptionSlider;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared layout and behaviour of the ArgentumForge video settings screens: title centred at the top, a two-column
 * grid of 150x20 controls (left column at centre - 155, right at centre + 5, rows 21 px apart, an odd last control
 * alone in the left column), sliders for numeric options and a 200 px Done button centred at the bottom.
 *
 * <p>Done and Esc return to the parent screen. Every close saves the ArgentumForge config and the vanilla options
 * ({@code GameSettings.saveOptions}, which the vanilla video screen does on Done and other clients may hook there).
 * Leaving the screens altogether applies pending reload options through {@link VideoSettingsSession}.</p>
 */
abstract class ArgentumSettingsScreen extends GuiScreen implements GuiSlider.ISlider {
    static final int DONE_ID = 200;
    private static final int FIRST_CONFIG_ID = 1000;
    private static final int COLUMN_OFFSET = 155;
    private static final int COLUMN_STEP = 160;
    private static final int ROW_HEIGHT = 21;
    private static final int TEXT_WIDTH = 142;

    protected final GuiScreen parent;
    private final String titleKey;
    private final List<Binding> bindings = new ArrayList<Binding>();
    private int slot;
    private boolean internalNavigation;

    ArgentumSettingsScreen(GuiScreen parent, String titleKey) {
        this.parent = parent;
        this.titleKey = titleKey;
    }

    /** Adds this page's controls with the add* methods, in grid order. */
    protected abstract void addControls();

    /** Page-specific buttons (page links, reset). */
    protected void onPageButton(GuiButton button) throws IOException {
    }

    @Override
    public void initGui() {
        super.initGui();
        internalNavigation = false;
        buttonList.clear();
        bindings.clear();
        slot = 0;
        addControls();
        buttonList.add(new GuiButton(DONE_ID, width / 2 - 100, height / 6 + 168, I18n.format("gui.done")));
        refreshStates();
    }

    // ---- layout ----

    private int slotX() {
        return width / 2 - COLUMN_OFFSET + (slot % 2) * COLUMN_STEP;
    }

    private int slotY() {
        return height / 6 + ROW_HEIGHT * (slot / 2) - 12;
    }

    /** Starts the next control on a new row (leaves the right column empty after an odd count). */
    protected void endRow() {
        if (slot % 2 == 1) slot++;
    }

    protected void addVanillaOption(GameSettings.Options option) {
        int id = option.returnEnumOrdinal();
        GuiButton button = option.getEnumFloat()
                ? new GuiOptionSlider(id, slotX(), slotY(), option)
                : new GuiOptionButton(id, slotX(), slotY(), option, mc.gameSettings.getKeyBinding(option));
        buttonList.add(button);
        slot++;
    }

    protected void addConfigOption(ConfigOption option) {
        Property property = option.property();
        ConfigOption.Kind kind = property == null ? null : ConfigOption.kindOf(property);
        if (kind == null) return;
        int id = FIRST_CONFIG_ID + bindings.size();
        GuiButton button;
        if (kind == ConfigOption.Kind.SLIDER) {
            int min = ConfigOption.min(property);
            int max = ConfigOption.max(property);
            String prefix = fit(label(option), Integer.toString(Math.abs(min) > Math.abs(max) ? min : max)) + ": ";
            button = new GuiSlider(id, slotX(), slotY(), 150, 20, prefix, "", min, max, property.getInt(), false, true, this);
        } else {
            button = new GuiButton(id, slotX(), slotY(), 150, 20, "");
        }
        Binding binding = new Binding(option, kind, button);
        bindings.add(binding);
        updateText(binding);
        buttonList.add(button);
        slot++;
    }

    protected GuiButton addPageButton(int id, String textKey) {
        GuiButton button = new GuiButton(id, slotX(), slotY(), 150, 20, I18n.format(textKey));
        buttonList.add(button);
        slot++;
        return button;
    }

    /** A disabled button used as a read-only status line. */
    protected void addStatus(int id, String text) {
        GuiButton button = new GuiButton(id, slotX(), slotY(), 150, 20, text);
        button.enabled = false;
        buttonList.add(button);
        slot++;
    }

    /** A button spanning both columns on its own row. */
    protected GuiButton addWideButton(int id, String textKey) {
        endRow();
        GuiButton button = new GuiButton(id, width / 2 - COLUMN_OFFSET, slotY(), 2 * COLUMN_OFFSET, 20, I18n.format(textKey));
        buttonList.add(button);
        slot += 2;
        return button;
    }

    // ---- text ----

    private static String label(ConfigOption option) {
        String text = I18n.format("argentumforge.option." + option.key);
        return option.reload ? text + " " + I18n.format("argentumforge.video.reload") : text;
    }

    /** Shortens the label so that "label: value" fits the 150 px button. */
    private String fit(String label, String value) {
        String text = label;
        while (text.length() > 4 && fontRendererObj.getStringWidth(text + ": " + value) > TEXT_WIDTH) {
            text = text.substring(0, text.length() - 1);
        }
        return text.equals(label) ? text : text.substring(0, text.length() - 1) + ".";
    }

    private void updateText(Binding binding) {
        if (binding.kind == ConfigOption.Kind.SLIDER) return;
        Property property = binding.option.property();
        if (property == null) return;
        String value;
        if (binding.kind == ConfigOption.Kind.TOGGLE) {
            value = I18n.format(property.getBoolean() ? "options.on" : "options.off");
        } else {
            value = I18n.format("argentumforge.value." + property.getString());
        }
        binding.button.displayString = fit(label(binding.option), value) + ": " + value;
    }

    /** Disables overlay-only options while the (pending) terrain mode is integrated. */
    protected void refreshStates() {
        boolean integrated = ConfigOption.pendingIntegratedMode();
        for (Binding binding : bindings) {
            binding.button.enabled = !(binding.option.overlayOnly && integrated) && isOptionEnabled(binding.option);
        }
    }

    /** Extra page-specific enable rule. */
    protected boolean isOptionEnabled(ConfigOption option) {
        return true;
    }

    // ---- input ----

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (button.id == DONE_ID) {
            back();
            return;
        }
        if (button instanceof GuiOptionButton && ((GuiOptionButton) button).returnEnumOptions() != null) {
            GameSettings.Options option = ((GuiOptionButton) button).returnEnumOptions();
            int guiScale = mc.gameSettings.guiScale;
            mc.gameSettings.setOptionValue(option, 1);
            button.displayString = mc.gameSettings.getKeyBinding(option);
            if (mc.gameSettings.guiScale != guiScale) {
                // Same as the vanilla video screen: re-layout for the new scaled resolution.
                ScaledResolution resolution = new ScaledResolution(mc);
                setWorldAndResolution(mc, resolution.getScaledWidth(), resolution.getScaledHeight());
            }
            return;
        }
        Binding binding = find(button);
        if (binding == null) {
            onPageButton(button);
            return;
        }
        Property property = binding.option.property();
        if (property == null) return;
        if (binding.kind == ConfigOption.Kind.TOGGLE) {
            property.set(!property.getBoolean());
        } else if (binding.kind == ConfigOption.Kind.CYCLE) {
            String[] valid = property.getValidValues();
            int next = 0;
            for (int i = 0; i < valid.length; i++) {
                if (valid[i].equalsIgnoreCase(property.getString())) next = (i + 1) % valid.length;
            }
            property.set(valid[next]);
        } else {
            return;
        }
        changed(binding);
    }

    @Override
    public void onChangeSliderValue(GuiSlider slider) {
        Binding binding = find(slider);
        // GuiSlider reports its initial value from its constructor, before the binding exists.
        if (binding == null) return;
        Property property = binding.option.property();
        if (property == null || property.getInt() == slider.getValueInt()) return;
        property.set(slider.getValueInt());
        changed(binding);
    }

    private void changed(Binding binding) {
        VideoSettingsSession.onChanged(binding.option.reload);
        updateText(binding);
        refreshStates();
    }

    private Binding find(GuiButton button) {
        for (Binding binding : bindings) {
            if (binding.button == button) return binding;
        }
        return null;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            back();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    // ---- navigation ----

    /** Opens a screen that returns here (a subpage or a confirmation); the session stays open. */
    protected void openChild(GuiScreen screen) {
        internalNavigation = true;
        mc.displayGuiScreen(screen);
    }

    protected void back() {
        internalNavigation = parent instanceof ArgentumSettingsScreen;
        mc.displayGuiScreen(parent);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Configuration cfg = ArgentumConfig.getConfiguration();
        if (cfg != null && cfg.hasChanged()) cfg.save();
        // This screen replaces the vanilla video settings screen, which saves the vanilla options on Done.
        mc.gameSettings.saveOptions();
        if (!internalNavigation) VideoSettingsSession.onLeft(mc);
        internalNavigation = false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, I18n.format(titleKey), width / 2, 15, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private static final class Binding {
        final ConfigOption option;
        final ConfigOption.Kind kind;
        final GuiButton button;

        Binding(ConfigOption option, ConfigOption.Kind kind, GuiButton button) {
            this.option = option;
            this.kind = kind;
            this.button = button;
        }
    }
}
