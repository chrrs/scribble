package me.chrr.scribble.screen;

import me.chrr.scribble.gui.BookTextWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.Optional;

@NullMarked
public class ScribbleLecternScreen extends ScribbleBookViewScreen implements MenuAccess<LecternMenu>, ContainerListener {
    private final LecternMenu menu;
    private int effectivePage = 0;

    public ScribbleLecternScreen(LecternMenu menu) {
        super(BookViewScreen.EMPTY_ACCESS);

        this.menu = menu;
        this.menu.addSlotListener(this);
    }

    @Override
    protected void initMenuControls(int y) {
        if (this.minecraft.player != null && this.minecraft.player.mayBuild()) {
            this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE,
                            button -> this.onClose())
                    .pos(this.width / 2 - 98 - 2, y).width(98).build());
            this.addRenderableWidget(Button.builder(Component.translatable("lectern.take_book"),
                            button -> this.sendButtonClick(LecternMenu.BUTTON_TAKE_BOOK))
                    .pos(this.width / 2 + 2, y).width(98).build());
        } else {
            super.initMenuControls(y);
        }
    }

    @Override
    public LecternMenu getMenu() {
        return this.menu;
    }

    @Override
    public void slotChanged(AbstractContainerMenu containerToSend, int dataSlotIndex, ItemStack stack) {
        this.book = Optional.ofNullable(BookViewScreen.BookAccess.fromItem(this.menu.getBook()))
                .orElse(BookViewScreen.EMPTY_ACCESS);

        if (this.currentPage >= this.book.getPageCount())
            this.showPage(this.book.getPageCount() - 1, false);

        this.updateCurrentPages();
    }

    @Override
    public void dataChanged(AbstractContainerMenu containerMenu, int dataSlotIndex, int value) {
        this.effectivePage = this.menu.getPage();
        this.showPage(this.effectivePage, false);
        this.updateEffectivePage();
    }

    @Override
    public void goPageForward(boolean toEnd) {
        if (toEnd) {
            this.jumpToPage(this.getTotalPages() - 1);
        } else {
            this.sendButtonClick(LecternMenu.BUTTON_NEXT_PAGE);
        }
    }

    @Override
    public void goPageBackward(boolean toStart) {
        if (toStart) {
            this.jumpToPage(0);
        } else {
            this.sendButtonClick(LecternMenu.BUTTON_PREV_PAGE);
        }
    }

    @Override
    public void jumpToPage(int page) {
        this.sendButtonClick(LecternMenu.BUTTON_PAGE_JUMP_RANGE_START + page);
    }

    @Override
    protected void closeRemoteContainer() {
        if (this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
    }

    @Override
    public void updateCurrentPages() {
        super.updateCurrentPages();
        this.updateEffectivePage();
    }

    private void updateEffectivePage() {
        // Dim the inactive pages.
        for (int i = 0; i < this.pagesToShow; i++) {
            int page = this.currentPage + i;
            boolean dimmed = page != this.effectivePage;

            ((BookTextWidget) this.textAreas.get(i)).setDimmed(dimmed);
            this.pageNumbers.get(i).setDimmed(dimmed);
        }

        // Override if the back/forward buttons are visible based on the effective page.
        if (this.forwardButton != null && this.backButton != null) {
            this.backButton.visible = this.effectivePage > 0;
            this.forwardButton.visible = this.effectivePage < this.getTotalPages() - 1;
        }
    }

    private void sendButtonClick(int pageData) {
        MultiPlayerGameMode gameMode = Objects.requireNonNull(this.minecraft.gameMode);
        gameMode.handleInventoryButtonClick(this.menu.containerId, pageData);
    }
}
