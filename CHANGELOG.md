## 1.6.3

- Update to 1.21.8.

## 1.6.2

- Make sure the text cursor is always positioned correctly (and visible).
- Re-fix keyboard shortcuts on alternate keyboard layouts.
- Better differentiate between hover and disabled action buttons.
- Make the 14th line accessible to write.
- Reset focus to the text edit box after turning the page.
- Replace reset (&r) color codes with black to prevent crashes.

Thanks to @J4PC for the bug reports!

## 1.6.1

- Update to 1.21.7.

## 1.6.0

- Update to 1.21.6 and drop support for older versions.
- Export books in a JSON-based format from now on.
    - All books saved to `.minecraft/books` will be converted automatically on startup.
    - NBT-based books will still be readable forever.
- Improve accessibility by a lot!

## 1.5.2

- Update Russian translation (Thanks to @FaNToMaSikkk for contributing!)
- Update to 1.21.5

*Note that this release is only for Fabric. Support for NeoForge will come later.*

## 1.5.1

- Improve compatability with mods changing the Narrator shortcut.
- Update Turkish, Mexican Spanish and German translations. (Thanks to @MissionWAR, @TheLegendOfSaram and @J4PC for
  contributing!)

## 1.5.0

- Allow for more granular control of when the action buttons are visible.
- Add a config option for the size of the history stack. (Thanks to @wiskiw for contributing!)
- Redesign the action buttons to be more minimal.
- Update Turkish translations. (Thanks to @MissionWAR for contributing!)

## 1.4.4

- Update icon and banner.
- Disallow keyboard focusing when editing books.
- Allow saving of written books.
- Add Russian translations. (Thanks to @FaNToMaSikkk for contributing!)
- Add German translations. (Thanks to @J4PC for contributing!)

## 1.4.3

- Update to 1.21.4.
- Add Simplified Chinese translations. (Thanks to @junshengxie for contributing!)

## 1.4.2

- Fix compatibility with Symbol Chat.
- Add a config option to hide the save/load buttons.
- Add Turkish translations. (Thanks to @MissionWAR for contributing!)

## 1.4.1-beta

- Add support for Forge and NeoForge.
- Add Traditional Chinese translations. (Thanks to @yichifauzi for contributing!)

## 1.4.0

- Add a config option to center book editing / reading GUI's vertically.

## 1.3.4

- Update to 1.21.3.

## 1.3.3

- Update to 1.21.2.
- Drop support for 1.20.3 to 1.20.6.

## 1.3.2

- Add a configuration option to copy without formatting by default.
- Fix mixin crash when Amendments is installed.
- Smaller bug fixes.

## 1.3.1

- Fix crash when typing when multiple colors are selected.
- Respect keyboard layout for undo and redo.
- Properly delete after cursor when pressing 'delete'.

## 1.3.0

- Allow for copying and pasting while keeping the formatting codes.
- Add the ability to undo and redo when pressing Ctrl-Z / Ctrl-Shift-Z.

Thanks to @wiskiw for the contributions!

## 1.2.2

- Add support for 1.21.1.

## 1.2.1

- Fix not being able to load a pick a book when trying to load from a file on macOS.
- Ask for confirmation before closing without saving.

## 1.2.0

- Add buttons to save books to files and load them.
- Don't insert a new page when there's already 100 pages.

## 1.1.2

- Skip to first or last page when holding SHIFT while pressing the next or previous page button.
- Properly position cursor after pasting text.
- Properly update selection when selecting at the end of a page.
- Properly remove empty pages when saving book.

## 1.1.1

- Fix Minecraft dependency string causing the mod to not load.

## 1.1.0

- Add a button to insert a new page before the current page.
- Fix compatibility with FixBookGUI.

## 1.0.1

- Change text cursor color to the current selected color.
- Toggle the narrator when pressing Ctrl-Shift-B.

## 1.0.0

- Add a button to delete the current page.
- Slightly shift the positions of some of the buttons.
- Support all versions from 1.20 up to 1.21

## 0.1.0-beta

- Initial release.