package me.chrr.scribble.mixture;

import me.chrr.scribble.book.SynchronizedPageList;

import java.util.ArrayList;
import java.util.List;

public class CommonMixture {

    public static List<String> mockPages(int size, String prefix) {
        ArrayList<String> list = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            list.add(prefix + "-page content:" + i);
        }

        return list;
    }

    public static List<String> mockPages(int size) {
        return mockPages(size, "none");
    }

    public static SynchronizedPageList mockSynchronizedPageList(int size) {
        SynchronizedPageList synchronizedPageList = new SynchronizedPageList();
        synchronizedPageList.populate(mockPages(size));
        return synchronizedPageList;
    }

}
