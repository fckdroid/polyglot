
package fckdroid.polyglot;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.concurrent.TimeUnit;

import fckdroid.polyglot.db.AppDatabase;
import fckdroid.polyglot.db.dao.LevelsDao;
import fckdroid.polyglot.db.dao.UsersDao;
import fckdroid.polyglot.db.dao.WordsDao;
import fckdroid.polyglot.model.User;
import fckdroid.polyglot.model.Word;
import fckdroid.polyglot.util.AppUtil;
import fckdroid.polyglot.util.UiUtil;
import fckdroid.polyglot.util.listener.HideKeyboardListener;
import fckdroid.polyglot.util.listener.ShowKeyboardListener;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;

public class GameActivity extends AppCompatActivity {
    private final int HIDE_TOOLBAR_DELAY = 150;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardVisibilityListener;
    private Disposable actionBarAnim = Disposables.disposed();
    private UsersDao usersDao;
    private WordsDao wordsDao;
    private Word currentWord;
    private LevelsDao levelsDao;
    private EditText etAnswer;
    private TextView tvLevel;
    private TextView tvWord;
    private TextView tvGrammar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        initDao();
        initUi();
        updateUi();
    }

    private void updateUi() {
        usersDao.loadUser()
                .map(User::getLevel)
                .flatMap(levelsDao::loadLevelById)
                .map(level -> getResources().getString(R.string.game_level, level.getLabel()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tvLevel::setText, Throwable::printStackTrace);

        usersDao.loadUser()
                .map(User::getLevel)
                .flatMap(wordsDao::loadNextWord)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onNextWord, Throwable::printStackTrace);
    }

    private void onNextWord(Word word) {
        currentWord = word;
        tvWord.setText(AppUtil.formatWord(word.getWord()));
        tvGrammar.setText(word.getGrammar().toLowerCase());
    }

    private void initDao() {
        usersDao = AppDatabase.getInstance(this).usersDao();
        wordsDao = AppDatabase.getInstance(this).wordsDao();
        levelsDao = AppDatabase.getInstance(this).levelsDao();
    }

    private void initUi() {
        FloatingActionButton fabSend = findViewById(R.id.game_fab_send);
        FloatingActionButton fabHint = findViewById(R.id.game_fab_hint);
        tvLevel = findViewById(R.id.game_tv_level);
        etAnswer = findViewById(R.id.game_et_answer);
        tvWord = findViewById(R.id.game_tv_word);
        tvGrammar = findViewById(R.id.game_tv_grammar);

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
        return () -> Completable.timer(HIDE_TOOLBAR_DELAY, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> actionBarAnim = disposable)
                .subscribe(() -> getSupportActionBar().hide());
    }
}
