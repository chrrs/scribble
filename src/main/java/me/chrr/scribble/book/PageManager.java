package me.chrr.scribble.book;


import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collection;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * - all public methods must be safe for execution
 */
public class PageManager {

    @Unique
    private static final int MAX_PAGES_NUMBER = 100;

    private final SynchronizedPageList pages;

    private final IntConsumer setCurrentPage;
    private final IntSupplier getCurrentPage;
    private final ChangeListener changeListener;

    public PageManager(
            SynchronizedPageList pages,
            IntConsumer setCurrentPage,
            IntSupplier getCurrentPage,
            ChangeListener changeListener
    ) {
        this.pages = pages;
        this.setCurrentPage = setCurrentPage;
        this.getCurrentPage = getCurrentPage;
        this.changeListener = changeListener;
    }

    /**
     * Update the link to the native pages collection and open first page
     *
     * <p>
     * - save to call with empty collection
     * - keep the current page index in bounds
     * - newPages should implement .add(String) method!
     *
     * @param newPages
     */
    public void setNativePages(List<String> newPages) {
//         TODO - Previous Implementation:
//        synchronizedPages.populate(this.pages);

        if (newPages.isEmpty()) {
            newPages.add(""); // to be able to safely open the first page
        }

        pages.populate(newPages);

        setCurrentPage.accept(0);
        changeListener.onPagesChanged(true);
    }

    /**
     * repopulate pages + open first page
     * <p>
     * - save to call with empty collection
     * - keep the current page index in bounds
     *
     * @param newPages
     */
    public void setPages(Collection<RichText> newPages) {
//        TODO - Previous Implementation:
//        Collection<RichText> loadedPages = bookFile.pages().isEmpty()
//                ? List.of(RichText.empty()) // if loaded book has no pages, then create an empty page
//                : bookFile.pages();
//
//        synchronizedPages.clear();
//        synchronizedPages.addAll(loadedPages);
//
//        this.currentPage = 0;
//        this.dirty = true;
//        this.updateButtons();
//        this.changePage();


        newPages = newPages.isEmpty()
                ? List.of(RichText.empty())  // if trying to populate with empty collection, then create an empty page
                : newPages;

        pages.clear();
        pages.addAll(newPages);

//        currentPage = 0; - called
        setCurrentPage.accept(0);

//        dirty = true; - called
//        updateButtons(); - called
//        changePage(); - called
        changeListener.onPagesChanged(true);

    }

    public List<RichText> getPages() {
//        TODO - Previous Implementation:
//        synchronizedPages.getRichPages()

        return pages.getRichPages();
    }

    public int getCurrentPageIndex() {
        return getCurrentPage.getAsInt();
    }

    public void openPage(int index) {
//        TODO - Previous Implementation:
//        currentPage = index

        setCurrentPage.accept(index);
        changeListener.onPagesChanged(true);
    }

    public boolean openFirstPage() {
//        TODO - Previous Implementation:
//        this.currentPage = 0;
//        this.updateButtons();
//        this.changePage();

        if (getCurrentPageIndex() != 0) {
//            currentPage = 0; - called
            setCurrentPage.accept(0);

//            dirty = true; - called, BUT WAS NOT CALLED BEFORE
//            updateButtons(); - called
//            changePage(); - called
            changeListener.onPagesChanged(true);

            return true;
        } else {
            return false;
        }
    }

    public boolean openLastPage() {
//        TODO - Previous Implementation:
//        int lastPage = synchronizedPages.size() - 1;
//        if (this.currentPage < lastPage && Screen.hasShiftDown()) {
//            this.currentPage = lastPage;
//            this.updateButtons();
//            this.changePage();
//        }

        int lastPageIndex = pages.size() - 1;
        if (getCurrentPageIndex() != lastPageIndex) {
//            currentPage = lastPageIndex; - called
            setCurrentPage.accept(lastPageIndex);

//            dirty = true; - called, BUT WAS NOT CALLED BEFORE
//            updateButtons(); - called
//            changePage(); - called
            changeListener.onPagesChanged(true);
            return true;
        } else {
            return false;
        }
    }

    public boolean canAddPage(int index) {
        boolean isPagesLimitExceeded = getPageCount() >= MAX_PAGES_NUMBER;
        boolean isIndexValid = index >= 0 && index <= getPageCount();

        return !isPagesLimitExceeded && isIndexValid;
    }

    public boolean canAddPageAtCurrentIndex() {
//        TODO - Previous Implementation:
//        synchronizedPages.size() < MAX_PAGES_NUMBER

        return canRemovePage(getCurrentPageIndex());
    }

    /**
     * safely insert new page into index
     * open inserted page
     *
     * @param index
     * @param page
     */
    public boolean insertPage(int index, RichText page) {
//        TODO - Previous Implementation:
//        synchronizedPages.add(index, RichText.empty());
//        --- AND ---
//        @Override
//        public void scribble$onPageAdded(int pageAddedIndex) {
//            currentPage = pageAddedIndex;
//            dirty = true;
//            updateButtons();
//            changePage();
//        }

        if (isCurrentPageIndexValid()) {
            pages.add(index, page);

//            currentPage = index; - called
            setCurrentPage.accept(index);

//            dirty = true; - called
//            updateButtons(); - called
//            changePage(); - called
            changeListener.onPagesChanged(true);

            return true;
        } else {
            return false;
        }
    }

    /**
     * trim only at the end
     */
    public void trimEmptyPages() {
//        TODO - Previous Implementation:
//        int lastIndex = synchronizedPages.size() - 1;
//        for (int i = lastIndex; i >= 0; i--) {
//            if (synchronizedPages.get(i).isEmpty()) {
//                synchronizedPages.remove(i);
//            } else {
//                // Break the loop as soon as we encounter a non-empty element
//                break;
//            }
//        }

        int lastIndex = pages.size() - 1;
        for (int i = lastIndex; i >= 0; i--) {
            if (pages.get(i).isEmpty()) {
                pages.remove(i);
            } else {
                // Break the loop as soon as we encounter a non-empty element
                break;
            }
        }

        // TODO Should we update the current page index,
        //  when trimmed page was open before? (the logic from removePage(int index))

        // TODO This method could change page number / current page index.
        //  Should we call onPagesChanged.accept(true) here?
    }

    public boolean canRemovePage(int index) {
        boolean isMinPageNumberExceeded = getPageCount() <= 1;
        return !isMinPageNumberExceeded && isIndexInBounds(index);
    }

    public boolean canRemoveCurrentPage() {
//        TODO - Previous Implementation:
//        synchronizedPages.size() > 1

        return canRemovePage(getCurrentPageIndex());
    }

    /**
     * returns removed page
     * returns null - if index is out of bounds
     *
     * @param index
     * @return
     */
    @Nullable
    public RichText removePage(int index) {
//        TODO - Previous Implementation:
//        deletedPage = synchronizedPages.get(index);
//        synchronizedPages.remove(index);
//        --- AND ---
//        @Override
//        public void scribble$onPageRemoved(int pageRemovedIndex) {
//            if (pageRemovedIndex < currentPage) {
//                currentPage = Math.max(0, pageRemovedIndex - 1);
//            } else if (currentPage >= synchronizedPages.size()) {
//                currentPage = Math.max(0, synchronizedPages.size() - 1);
//            }
//
//            dirty = true;
//            updateButtons();
//            changePage();
//        }

        RichText deletedPage = pages.get(index);
        pages.remove(index);

        int currentPageIndex = getCurrentPageIndex();
        if (index < currentPageIndex) {
            // if a page before the opened was removed
            // move the current opened page index to the left by 1 to keep the same page open
            int newPageIndex = Math.max(0, currentPageIndex - 1);

//            currentPage = newPageIndex; - called
            setCurrentPage.accept(newPageIndex);

        } else if (currentPageIndex >= getPageCount()) {
            // if after a page removing the current page index is out of right bound
            // open the last possible page
            int newPageIndex = Math.max(0, getPageCount() - 1);

//            currentPage = newPageIndex; - called
            setCurrentPage.accept(newPageIndex);
        }

//        dirty = true; - called
//        updateButtons(); - called
//        changePage(); - called
        changeListener.onPagesChanged(true);

        return deletedPage;
    }

    private boolean isCurrentPageIndexValid() {
        return isIndexInBounds(getCurrentPageIndex());
    }

    public boolean isIndexInBounds(int index) {
        return index >= 0 && index < pages.size();
    }

    public boolean arePagesEmpty() {
//        TODO - Previous Implementation:
//        synchronizedPages.arePagesEmpty()

        for (int i = 0; i < pages.size(); i++) {
            RichText page = pages.get(i);
            if (!page.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public int getPageCount() {
        return pages.size();
    }

    public RichText getCurrentPage() {
//        TODO - Previous Implementations:
//        return this.currentPage >= 0 && this.currentPage < synchronizedPages.size()
//                ? synchronizedPages.get(this.currentPage)
//                : RichText.empty();
//        --- OR ---
//        synchronizedPages.get(page).getPlainText();


        return isCurrentPageIndexValid()
                ? pages.get(getCurrentPageIndex())
                : RichText.empty();
    }

    /**
     * RichText replacement for {@link net.minecraft.client.gui.screen.ingame.BookEditScreen#setPageContent(String)}
     */
    public void setCurrentPage(RichText text) {
//        TODO - Previous Implementation:
//        if (this.currentPage >= 0 && this.currentPage < synchronizedPages.size()) {
//            synchronizedPages.set(this.currentPage, newText);
//        }
//
//        this.dirty = true;
//        this.invalidatePageContent();

        if (isCurrentPageIndexValid()) {
            pages.set(getCurrentPageIndex(), text);
        }

//      dirty = true; - called
//      updateButtons(); - called, BUT WAS NOT CALLED BEFORE
//      changePage(); - called, BUT WAS NOT CALLED BEFORE
//      invalidatePageContent(); - called (as a part of the BookEditScreen#changePage() implementation)
        changeListener.onPagesChanged(true);
    }

    public interface ChangeListener {

        void onPagesChanged(boolean invalidateCurrentPage);
    }
}
