package me.chrr.tapestry.config.gui;

import me.chrr.tapestry.config.Config;
import me.chrr.tapestry.config.Option;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public class TapestryConfigScreen extends Screen {
    private final Config config;
    private final Screen parent;

    private final List<OptionProxy<?>> proxies = new ArrayList<>();

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private @Nullable OptionList list;

    public TapestryConfigScreen(Config config, Screen parent) {
        super(config.getText("title"));

        this.config = config;
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);

        LinearLayout footerLayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footerLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, (button) -> this.onClose()).build());
        footerLayout.addChild(Button.builder(CommonComponents.GUI_DONE, (button) -> this.saveAndClose()).build());

        this.list = this.layout.addToContents(new OptionList(this.minecraft, this.width, this.layout.getContentHeight(), this.layout.getHeaderHeight()));
        for (Option<?, ?> option : this.config.getOptions()) {
            if (option.header != null)
                this.list.addHeader(option.header);
            this.proxies.add(this.list.addOption(option));
        }

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();

        if (this.list != null)
            this.list.updateSize(this.width, this.layout);
    }

    public void saveAndClose() {
        this.proxies.forEach(OptionProxy::apply);
        this.config.save();
        this.onClose();
    }

    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}

