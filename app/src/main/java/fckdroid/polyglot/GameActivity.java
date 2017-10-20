
package fckdroid.polyglot;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.transition.ChangeBounds;
import android.support.transition.Explode;
import android.support.transition.Transition;
import android.support.transition.TransitionListenerAdapter;
import android.support.transition.TransitionManager;
import android.support.transition.TransitionSet;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.concurrent.TimeUnit;

import fckdroid.polyglot.db.AppDatabase;
import fckdroid.polyglot.db.dao.LevelsDao;
import fckdroid.polyglot.db.dao.UsersDao;
import fckdroid.polyglot.db.dao.WordsDao;
import fckdroid.polyglot.db.entity.UserEntity;
import fckdroid.polyglot.model.Level;
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
    private static final int HIDE_TOOLBAR_DELAY = 150;
    public boolean transitionInWork;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardVisibilityListener;
    private Disposable actionBarAnim = Disposables.disposed();
    private UsersDao usersDao;
    private WordsDao wordsDao;
    private User currentUser;
    private Level currentLevel;
    private Word currentWord;
    private LevelsDao levelsDao;
    private ViewGroup viewGroup;
    private EditText etAnswer;
    private TextView tvLevel;
    private TextView tvWord;
    private TextView tvGrammar;
    private TextView tvHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        initUi();
        initDao();
        updateUi();
    }

    private void updateUi() {
        usersDao.loadUser()
                .doOnSuccess(user -> currentUser = user)
                .map(User::getLevel)
                .flatMap(levelsDao::loadLevelById)
                .doOnSuccess(level -> this.currentLevel = level)
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
        viewGroup = findViewById(R.id.game_viewgroup);
        tvLevel = findViewById(R.id.game_tv_level);
        etAnswer = findViewById(R.id.game_et_answer);
        tvWord = findViewById(R.id.game_tv_word);
        tvGrammar = findViewById(R.id.game_tv_grammar);
        tvHint = findViewById(R.id.game_tv_hint);
        tvHint = findViewById(R.id.game_tv_hint);

        fabHint.setOnClickListener(v -> {
            fabHint.hide();
            animateChangingBounds();
            tvHint.setText(currentWord.getHint());
            tvHint.setVisibility(View.VISIBLE);
            currentUser.onHintClick(currentLevel.getRate());
            Completable.fromAction(() -> usersDao.updateUser((UserEntity) currentUser))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {/*ignore*/}, Throwable::printStackTrace);
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
        tvHint.setVisibility(View.GONE);
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

    @NonNull
    private HideKeyboardListener onHideKeyboard() {
        return () -> startDelayedAnimation(this::onContentExpand);
    }

    @NonNull
    private ShowKeyboardListener onShowKeyboard() {
        return () -> startDelayedAnimation(this::onContentCollapse);
    }

    @NonNull
    private Disposable startDelayedAnimation(Runnable method) {
        return Completable.timer(HIDE_TOOLBAR_DELAY, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> {
                    actionBarAnim.dispose();
                    actionBarAnim = disposable;
                })
                .subscribe(method::run);
    }

    @SuppressWarnings("ConstantConditions")
    private void onContentCollapse() {
        if (tvLevel.getVisibility() != View.GONE) {
            getSupportActionBar().hide();
            animateChangingBounds();
            tvLevel.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void onContentExpand() {
        if (tvLevel.getVisibility() != View.VISIBLE) {
            animateChangingBounds();
            tvLevel.setVisibility(View.VISIBLE);
        }
        getSupportActionBar().show();
    }

    private void animateChangingBounds() {
        if (transitionInWork) {
            return;
        }
        TransitionManager.beginDelayedTransition(viewGroup, new TransitionSet()
                .addTransition(new ChangeBounds())
                .addTransition(new Explode())
                .addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionEnd(@NonNull Transition transition) {
                        super.onTransitionEnd(transition);
                        transitionInWork = false;
                    }

                    @Override
                    public void onTransitionStart(@NonNull Transition transition) {
                        super.onTransitionStart(transition);
                        transitionInWork = true;
                    }
                }));
    }
}
