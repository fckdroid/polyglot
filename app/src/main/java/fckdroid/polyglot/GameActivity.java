
package fckdroid.polyglot;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.FloatingActionButton;
import android.support.transition.AutoTransition;
import android.support.transition.ChangeBounds;
import android.support.transition.Fade;
import android.support.transition.TransitionManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
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
import fckdroid.polyglot.util.AndroidUtil;
import fckdroid.polyglot.util.AppUtil;
import fckdroid.polyglot.util.TextValidator;
import fckdroid.polyglot.util.listener.HideKeyboardListener;
import fckdroid.polyglot.util.listener.ShowKeyboardListener;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;

import static fckdroid.polyglot.R.id.game_fab_send;
import static fckdroid.polyglot.util.AppUtil.EMPTY_STRING;

public class GameActivity extends AppCompatActivity {
    public static final String SPACE = " ";
    private static final int SKIP_THROTTLE = 500;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardVisibilityListener;
    private Disposable actionBarAnim = Disposables.disposed();
    private CompositeDisposable compositeDisposable;
    private UsersDao usersDao;
    private WordsDao wordsDao;
    private User currentUser;
    private Word currentWord;
    private Level currentLevel;
    private Level prevLevel;
    private Level nextLevel;
    private LevelsDao levelsDao;
    private ConstraintLayout constraintLayout;
    private EditText etAnswer;
    private TextView tvScore;
    private TextView tvWord;
    private TextView tvGrammar;
    private TextView tvHint;
    private FloatingActionButton fabSend;
    private FloatingActionButton fabHint;
    private boolean answerAttempt = true;
    private View tvSkip;
    private View btnMinus;
    private View btnPlus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        compositeDisposable = new CompositeDisposable();
        initViews();
        initDao();
        updateUi();
    }

    private void updateUi() {
        /*Create connectable observer for User instance*/
        ConnectableObservable<UserEntity> userConnectableObserver = usersDao.loadUser()
                .subscribeOn(Schedulers.io())
                .doOnSuccess(user -> currentUser = user)
                .toObservable()
                .publish();

        /*Init current level*/
        userConnectableObserver
                .map(User::getLevel)
                .flatMapSingle(levelsDao::loadLevelById)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onNewtLevel, Throwable::printStackTrace);

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
                .toMaybe()
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe(level -> prevLevel = level, Throwable::printStackTrace);

        /*Init next level*/
        Single.just(currentLevel)
                .map(levelId -> ++levelId)
                .filter(prevLevelId -> prevLevelId <= 5 && currentLevel < 5)
                .onErrorComplete()
                .flatMapSingle(levelsDao::loadLevelById)
                .toMaybe()
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe(level -> nextLevel = level, Throwable::printStackTrace);
    }

    private void onNextWord(Word word) {
        currentWord = word;
        tvWord.setText(AppUtil.formatWord(word.getWord()));
        tvGrammar.setText(word.getGrammar().toLowerCase());
        TransitionManager.beginDelayedTransition(constraintLayout, new Fade());
        tvWord.setVisibility(View.VISIBLE);
        tvGrammar.setVisibility(View.VISIBLE);
        if (!currentWord.getHint().isEmpty()) {
            fabHint.show();
        } else {
            fabHint.hide();
        }
    }

    private void onNewtLevel(Level level) {
        currentLevel = level;
        updateScore();
        String levelLabel = getResources().getString(R.string.game_level, level.getLabel());
        getSupportActionBar().setTitle(levelLabel);
        initNextAndPrevLevels(level.getId());
    }

    private void initDao() {
        usersDao = AppDatabase.getInstance(this).usersDao();
        wordsDao = AppDatabase.getInstance(this).wordsDao();
        levelsDao = AppDatabase.getInstance(this).levelsDao();
    }

    private void initViews() {
        fabSend = findViewById(game_fab_send);
        fabHint = findViewById(R.id.game_fab_hint);
        constraintLayout = findViewById(R.id.game_viewgroup);
        tvScore = findViewById(R.id.game_tv_score);
        etAnswer = findViewById(R.id.game_et_answer);
        tvWord = findViewById(R.id.game_tv_word);
        tvGrammar = findViewById(R.id.game_tv_grammar);
        tvHint = findViewById(R.id.game_tv_hint);
        tvSkip = findViewById(R.id.game_tv_skip);
        btnPlus = findViewById(R.id.btn_plus);
        btnMinus = findViewById(R.id.btn_minus);

        fabSend.setVisibility(View.INVISIBLE);
        tvHint.setVisibility(View.GONE);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(EMPTY_STRING);
    }

    private void initListeners() {
        btnPlus.setOnClickListener(v -> {
            if (currentUser.onRightAnswer(currentLevel.getRate(), nextLevel)) {
                onNewtLevel(nextLevel);
            }
            saveUser().subscribe(() -> animateOnNextWord(this::updateUi), Throwable::printStackTrace);
        });
        btnMinus.setOnClickListener(v -> {
            if (currentUser.onWrongAnswer(currentLevel.getRate(), prevLevel)) {
                onNewtLevel(prevLevel);
            }
            saveUser().subscribe(() -> animateOnNextWord(this::updateUi), Throwable::printStackTrace);
        });
        keyboardVisibilityListener = AndroidUtil.getKeyboardVisibilityListener(
                etAnswer, onShowKeyboard(), onHideKeyboard());
        etAnswer.addTextChangedListener(new TextValidator(etAnswer) {
            @Override
            public void validate(TextView textView, String text) {
                if (text.startsWith(SPACE)) {
                    textView.setText(text.replace(SPACE, EMPTY_STRING));
                }
            }
        });
        etAnswer.getViewTreeObserver().addOnGlobalLayoutListener(keyboardVisibilityListener);

        fabSend.setOnClickListener(view -> {
            String userAnswer = etAnswer.getText().toString().toLowerCase();

            if (AppUtil.checkAnswer(currentWord.getTranslation(), userAnswer)) {
                if (currentUser.onRightAnswer(currentLevel.getRate(), nextLevel)) {
                    onNewtLevel(nextLevel);
                }
                onNextWord(false);
            } else {
                if (answerAttempt) {
                    Toast.makeText(this, "Осталась 1 попытка", Toast.LENGTH_SHORT).show();
                    answerAttempt = false;
                } else {
                    if (currentUser.onWrongAnswer(currentLevel.getRate(), prevLevel)) {
                        onNewtLevel(prevLevel);
                    }
                    onNextWord(false);
                    answerAttempt = true;
                }
            }

            saveUser().subscribe(this::updateScore, Throwable::printStackTrace);
        });

        fabHint.setOnClickListener(v -> {
            currentLevel.onHintClick();
            fabHint.hide();
            TransitionManager.beginDelayedTransition(constraintLayout, new AutoTransition());
            tvHint.setText(currentWord.getHint());
            tvHint.setVisibility(View.VISIBLE);
        });

        RxView.clicks(tvSkip)
                .throttleFirst(SKIP_THROTTLE, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(compositeDisposable::add)
                .subscribe(ignoreIt -> onNextWord(true));

        RxTextView.textChanges(etAnswer)
                .skip(1)
                .map(Object::toString)
                .filter(input -> !input.startsWith(SPACE))
                .map(TextUtils::isEmpty)
                .doOnSubscribe(compositeDisposable::add)
                .subscribe(this::onSendOrSkipClick);
    }

    private void updateScore() {
        TransitionManager.endTransitions(constraintLayout);
        String score = getResources().getString(R.string.game_score, currentUser.getScore());
        tvScore.setText(score);
    }

    private void onSendOrSkipClick(boolean isEmpty) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);
        if (isEmpty && fabSend.isShown()) {
            constraintSet.connect(tvSkip.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0);
            fabSend.hide();
            TransitionManager.beginDelayedTransition(constraintLayout, new ChangeBounds());
            constraintSet.applyTo(constraintLayout);
        } else if (!isEmpty && !fabSend.isShown()) {
            constraintSet.connect(tvSkip.getId(), ConstraintSet.LEFT, R.id.guideline_vertical, ConstraintSet.LEFT, 0);
            fabSend.show();
            TransitionManager.beginDelayedTransition(constraintLayout, new ChangeBounds());
            constraintSet.applyTo(constraintLayout);
        }
    }

    private void onNextWord(boolean isSkipped) {
        if (isSkipped) {
            if (currentUser.onSkipWord(currentLevel.getRate(), prevLevel)) {
                onNewtLevel(prevLevel);
            }
            saveUser().subscribe(() -> animateOnNextWord(this::updateUi), Throwable::printStackTrace);
        } else {
            animateOnNextWord(this::updateUi);
        }
        etAnswer.setText(EMPTY_STRING);
    }

    private Completable saveUser() {
        return Completable.fromAction(() -> usersDao.updateUser((UserEntity) currentUser))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void animateOnNextWord(Runnable action) {
        tvHint.setVisibility(View.GONE);
        int delay = fabSend.isShown() ? 350 : 0;
        Completable.timer(delay, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(action::run);
        tvWord.setVisibility(View.INVISIBLE);
        tvGrammar.setVisibility(View.INVISIBLE);
        tvHint.setVisibility(View.INVISIBLE);
        fabHint.hide();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        fabSend.setOnClickListener(null);
        fabHint.setOnClickListener(null);
        compositeDisposable.dispose();
        etAnswer.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardVisibilityListener);
        keyboardVisibilityListener = null;
        actionBarAnim.dispose();
    }

    @SuppressWarnings("ConstantConditions")
    private HideKeyboardListener onHideKeyboard() {
        return () -> getSupportActionBar().show();
    }

    @SuppressWarnings("ConstantConditions")
    private ShowKeyboardListener onShowKeyboard() {
        return () -> getSupportActionBar().hide();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
        }
        return true;
    }
}
