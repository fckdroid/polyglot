package fckdroid.polyglot.util;


import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;

import fckdroid.polyglot.util.listener.HideKeyboardListener;
import fckdroid.polyglot.util.listener.ShowKeyboardListener;

public class AndroidUtil {
    private AndroidUtil() { }

    public static ViewTreeObserver.OnGlobalLayoutListener getKeyboardVisibilityListener(View view,
                                                                                        ShowKeyboardListener showKeyboardListener,
                                                                                        HideKeyboardListener hideKeyboardListener) {
        return () -> {
            Rect r = new Rect();
            int screenHeight = view.getRootView().getHeight();
            view.getRootView().getWindowVisibleDisplayFrame(r);
            int heightDifference = screenHeight - (r.bottom - r.top);
            if (heightDifference > screenHeight / 3) {
                showKeyboardListener.onShowKeyboard();
            } else {
                hideKeyboardListener.onHideKeyboard();
            }
        };
    }
}
