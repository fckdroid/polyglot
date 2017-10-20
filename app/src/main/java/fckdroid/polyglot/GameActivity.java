
package fckdroid.polyglot;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;

import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.concurrent.TimeUnit;

import fckdroid.polyglot.db.AppDatabase;
import fckdroid.polyglot.util.UiUtil;
import fckdroid.polyglot.util.listener.HideKeyboardListener;
import fckdroid.polyglot.util.listener.ShowKeyboardListener;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;

public class GameActivity extends AppCompatActivity {
    private ViewTreeObserver.OnGlobalLayoutListener keyboardVisibilityListener;
    private EditText etAnswer;
    private Disposable actionBarAnim = Disposables.disposed();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        initUi();
        AppDatabase.getInstance(this).userDao().loadUser().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(userEntity -> {},
                        throwable -> {});
    }

    private void initUi() {
        FloatingActionButton fabSend = findViewById(R.id.game_fab_send);
        FloatingActionButton fabHint = findViewById(R.id.game_fab_hint);
        etAnswer = findViewById(R.id.game_et_answer);

        fabHint.setOnClickListener(v -> {
            fabHint.hide();
        });

        RxTextView.textChanges(etAnswer)
                .map(TextUtils::isEmpty)
                .subscribe(isEmpty -> {
                    if (isEmpty) {
                        fabSend.hide();
                    } else {
                        fabSend.show();
                    }
                });

        fabSend.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        keyboardVisibilityListener = UiUtil.getKeyboardVisibilityListener(etAnswer, onShowKeyboard(), onHideKeyboard());
        etAnswer.getViewTreeObserver().addOnGlobalLayoutListener(keyboardVisibilityListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        etAnswer.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardVisibilityListener);
        keyboardVisibilityListener = null;
        actionBarAnim.dispose();
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    private HideKeyboardListener onHideKeyboard() {
        return () -> getSupportActionBar().show();
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    private ShowKeyboardListener onShowKeyboard() {
        return () -> Completable.timer(150, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> actionBarAnim = disposable)
                .subscribe(() -> getSupportActionBar().hide());
    }
}
