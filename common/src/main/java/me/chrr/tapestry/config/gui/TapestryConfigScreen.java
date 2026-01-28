package me.chrr.tapestry.config.gui;

import me.chrr.tapestry.config.Config;
import me.chrr.tapestry.config.Option;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@NullMarked
public class TapestryConfigScreen extends Screen {
    private final Config[] configs;
    private final Screen parent;

    private final List<OptionProxy<?>> proxies = new ArrayList<>();

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);

    private @Nullable TabNavigationBar tabNavigationBar;
    private @Nullable OptionList optionList;

    public TapestryConfigScreen(Screen parent, Config... configs) {
        super(Component.empty());
        assert configs.length > 0;

        this.configs = configs;
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Initialise the actual option lists.
        ConfigTab[] tabs = new ConfigTab[this.configs.length];
        for (int i = 0; i < this.configs.length; i++) {
            OptionList list = createOptionList(this.configs[i], this.width,
                    this.layout.getContentHeight(), this.layout.getHeaderHeight());
            tabs[i] = new ConfigTab(this.configs[i], list);
        }

        if (tabs.length == 1) {
            // If we have only a single config, just show a simple title header.
            this.layout.addTitleHeader(tabs[0].getTabTitle(), this.font);
            this.optionList = this.layout.addToContents(createOptionList(tabs[0].config, this.width,
                    this.layout.getContentHeight(), this.layout.getHeaderHeight()));
        } else {
            // If we have more than one config, show them as tabs.
            this.tabNavigationBar = this.addRenderableWidget(
                    TabNavigationBar.builder(this.tabManager, this.width).addTabs(tabs).build());
            this.tabNavigationBar.selectTab(0, false);
        }

        // Add the footer cancel and done buttons.
        LinearLayout footerLayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footerLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, (button) -> this.onClose()).build());
        footerLayout.addChild(Button.builder(CommonComponents.GUI_DONE, (button) -> this.saveAndClose()).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        if (this.optionList != null) {
            this.layout.arrangeElements();
            this.optionList.updateSize(this.width, this.layout);
        } else if (this.tabNavigationBar != null) {
            this.tabNavigationBar.updateWidth(this.width);
            this.tabNavigationBar.arrangeElements();

            int headerHeight = this.tabNavigationBar.getRectangle().bottom();
            ScreenRectangle screenRectangle = new ScreenRectangle(0, headerHeight,
                    this.width, this.height - headerHeight - this.layout.getFooterHeight());
            this.tabManager.setTabArea(screenRectangle);
            this.layout.setHeaderHeight(headerHeight);
            this.layout.arrangeElements();
        }
    }

    private OptionList createOptionList(Config config, int width, int height, int y) {
        OptionList list = new OptionList(this.minecraft, this.configs.length <= 1, width, height, y);
        for (Option<?> option : config.getOptions()) {
            if (option.header != null)
                list.addHeader(option.header);

            if (!option.hidden)
                this.proxies.add(list.addOption(option));
        }

        return list;
    }

    public void saveAndClose() {
        this.proxies.forEach(OptionProxy::apply);
        for (Config config : this.configs)
            config.save();

        this.onClose();
    }

    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    public record ConfigTab(Config config, OptionList list) implements Tab {
        @Override
        public Component getTabTitle() {
            return this.config.getTitle();
        }

        @Override
        public Component getTabExtraNarration() {
            return Component.empty();
        }

        @Override
        public void visitChildren(Consumer<AbstractWidget> consumer) {
            consumer.accept(this.list);
        }

        @Override
        public void doLayout(ScreenRectangle rectangle) {
            this.list.updateSizeAndPosition(rectangle.width(), rectangle.height(), rectangle.top());
        }
    }
}

