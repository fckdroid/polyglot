
package fckdroid.polyglot;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.transition.ChangeBounds;
import android.support.transition.Explode;
import android.support.transition.Fade;
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
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
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
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;

import static fckdroid.polyglot.util.AppUtil.EMPTY_STRING;

public class GameActivity extends AppCompatActivity {
    private static final int SKIP_THROTTLE = 500;
    private boolean transitionInWork;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardVisibilityListener;
    private Disposable actionBarAnim = Disposables.disposed();
    private UsersDao usersDao;
    private WordsDao wordsDao;
    private User currentUser;
    private Word currentWord;
    private Level currentLevel;
    private Level prevLevel;
    private Level nextLevel;
    private LevelsDao levelsDao;
    private ViewGroup viewGroup;
    private EditText etAnswer;
    private TextView tvLevel;
    private TextView tvWord;
    private TextView tvGrammar;
    private TextView tvHint;
    private FloatingActionButton fabSend;
    private FloatingActionButton fabHint;
    private boolean oneMoreAttemptToAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        initViews();
        initListeners();
        initDao();
        updateUi();
    }

    private void updateUi() {
        /*Create connectable observer for User instance*/
        ConnectableObservable<UserEntity> userConnectableObserver = usersDao.loadUser()
                .doOnSuccess(user -> currentUser = user)
                .toObservable()
                .subscribeOn(Schedulers.io())
                .publish();

        /*Init current level*/
        userConnectableObserver
                .map(User::getLevel)
                .flatMapSingle(levelsDao::loadLevelById)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(level -> initNextAndPrevLevels(level.getId()))
                .subscribe(level -> onNextLevel(level, false), Throwable::printStackTrace);

        /*Init word*/
        userConnectableObserver
                .map(User::getLevel)
                .flatMapSingle(wordsDao::loadNextWord)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onNextWord, Throwable::printStackTrace);

        userConnectableObserver.connect();
    }

    private void initNextAndPrevLevels(long currentLevel) {
        /*Init previous level*/
        Single.just(currentLevel)
                .map(levelId -> --levelId)
                .filter(prevLevelId -> prevLevelId >= 0 && currentLevel > 0)
                .flatMapSingle(levelsDao::loadLevelById)
                .subscribeOn(Schedulers.io())
                .subscribe(level -> prevLevel = level, Throwable::printStackTrace);

        /*Init next level*/
        Single.just(currentLevel)
                .map(levelId -> ++levelId)
                .filter(prevLevelId -> prevLevelId <= 5 && currentLevel < 5)
                .flatMapSingle(levelsDao::loadLevelById)
                .subscribeOn(Schedulers.io())
                .subscribe(level -> nextLevel = level, Throwable::printStackTrace);
    }

    private void onNextWord(Word word) {
        currentWord = word;
        tvWord.setText(AppUtil.formatWord(word.getWord()));
        tvGrammar.setText(word.getGrammar().toLowerCase());
        TransitionManager.beginDelayedTransition(viewGroup, new Fade());
        tvWord.setVisibility(View.VISIBLE);
        tvGrammar.setVisibility(View.VISIBLE);
        fabHint.show();
        oneMoreAttemptToAnswer = true;
    }

    private void onNextLevel(Level level, boolean animate) {
        currentLevel = level;
        String levelLabel = getResources().getString(R.string.game_level, level.getLabel());
        tvLevel.setText(levelLabel);
        if (animate) {
            initNextAndPrevLevels(level.getId());
        }
    }

    private void initDao() {
        usersDao = AppDatabase.getInstance(this).usersDao();
        wordsDao = AppDatabase.getInstance(this).wordsDao();
        levelsDao = AppDatabase.getInstance(this).levelsDao();
    }

    private void initViews() {
        fabSend = findViewById(R.id.game_fab_send);
        fabHint = findViewById(R.id.game_fab_hint);
        viewGroup = findViewById(R.id.game_viewgroup);
        tvLevel = findViewById(R.id.game_tv_level);
        etAnswer = findViewById(R.id.game_et_answer);
        tvWord = findViewById(R.id.game_tv_word);
        tvGrammar = findViewById(R.id.game_tv_grammar);
        tvHint = findViewById(R.id.game_tv_hint);
        fabSend.setVisibility(View.INVISIBLE);
        tvHint.setVisibility(View.GONE);
        tvLevel.setVisibility(View.INVISIBLE);
    }

    private void initListeners() {
        fabSend.setOnClickListener(view -> {
            String userAnswer = etAnswer.getText().toString().toLowerCase();

            if (AppUtil.checkAnswer(currentWord.getTranslation(), userAnswer)) {
                onNextWord(false);
                if (currentUser.onRightAnswer(currentLevel.getRate(), nextLevel)) {
                    onNextLevel(nextLevel, true);
                }
            } else {
                if (oneMoreAttemptToAnswer) {
                    Toast.makeText(this, "Осталась 1 попытка", Toast.LENGTH_SHORT).show();
                    oneMoreAttemptToAnswer = false;
                }
                if (currentUser.onWrongAnswer(currentLevel.getRate(), prevLevel)) {
                    onNextLevel(prevLevel, true);
                }
            }

            Completable.fromAction(() -> usersDao.updateUser((UserEntity) currentUser))
                    .subscribeOn(Schedulers.io())
                    .subscribe(() -> {/*ignore*/}, Throwable::printStackTrace);
        });

        fabHint.setOnClickListener(v -> {
            currentLevel.onHintClick();
            fabHint.hide();
            animateOnChangeBounds();
            tvHint.setText(currentWord.getHint());
            tvHint.setVisibility(View.VISIBLE);
        });

        RxView.clicks(findViewById(R.id.game_tv_skip))
                .throttleFirst(SKIP_THROTTLE, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ignored -> onNextWord(true));

        RxTextView.textChanges(etAnswer)
                .map(TextUtils::isEmpty)
                .subscribe(isEmpty -> {
                    if (isEmpty) {
                        fabSend.hide();
                    } else {
                        fabSend.show();
                    }
                });
    }

    private void onNextWord(boolean isSkipped) {
        if (isSkipped) {
            currentUser.onSkipWord(currentLevel.getRate());
            Completable.fromAction(() -> usersDao.updateUser((UserEntity) currentUser))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::animateOnSkip, Throwable::printStackTrace);
        } else {
            animateOnSkip();
        }
        etAnswer.setText(EMPTY_STRING);
    }

    private void animateOnSkip() {
        TransitionManager.beginDelayedTransition(viewGroup, new TransitionSet()
                .addTransition(new Explode())
                .addTransition(new Fade())
                .addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionEnd(@NonNull Transition transition) {
                        super.onTransitionEnd(transition);
                        tvHint.setVisibility(View.GONE);
                        updateUi();
                    }
                }));
        tvWord.setVisibility(View.INVISIBLE);
        tvGrammar.setVisibility(View.INVISIBLE);
        tvHint.setVisibility(View.INVISIBLE);
        fabHint.hide();
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
    private HideKeyboardListener onHideKeyboard() {
        return () -> {
            if (tvLevel.getVisibility() != View.VISIBLE) {
                animateOnChangeBounds();
                tvLevel.setVisibility(View.VISIBLE);
            }
            getSupportActionBar().show();
        };
    }

    @SuppressWarnings("ConstantConditions")
    private ShowKeyboardListener onShowKeyboard() {
        return () -> {
            if (tvLevel.getVisibility() != View.GONE) {
                getSupportActionBar().hide();
                animateOnChangeBounds();
                tvLevel.setVisibility(View.GONE);
            }
        };
    }

    private void animateOnChangeBounds() {
        if (!transitionInWork) {
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
}
