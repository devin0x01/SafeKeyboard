package com.valence.safe.keyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Valence on 2018/3/7 0007.
 */

public class SafeKeyboard {

    private static final String TAG = "SafeKeyboard";
    private static final SparseArray<String> letterBindArray;
    private static final HashMap<String, String> letter2CodeMap;
    private final SafeKeyboardConfig keyboardConfig;

    static {
        letterBindArray = new SparseArray<>();
        letter2CodeMap = new HashMap<>();
        letterBindArray.put(0, "a");
        letterBindArray.put(1, "b");
        letterBindArray.put(2, "c");
        letterBindArray.put(3, "d");
        letterBindArray.put(4, "e");
        letterBindArray.put(5, "f");
        letterBindArray.put(6, "g");
        letterBindArray.put(7, "h");
        letterBindArray.put(8, "i");
        letterBindArray.put(9, "j");
        letterBindArray.put(10, "k");
        letterBindArray.put(11, "l");
        letterBindArray.put(12, "m");
        letterBindArray.put(13, "n");
        letterBindArray.put(14, "o");
        letterBindArray.put(15, "p");
        letterBindArray.put(16, "q");
        letterBindArray.put(17, "r");
        letterBindArray.put(18, "s");
        letterBindArray.put(19, "t");
        letterBindArray.put(20, "u");
        letterBindArray.put(21, "v");
        letterBindArray.put(22, "w");
        letterBindArray.put(23, "x");
        letterBindArray.put(24, "y");
        letterBindArray.put(25, "z");
        letter2CodeMap.put("a", "97");
        letter2CodeMap.put("b", "98");
        letter2CodeMap.put("c", "99");
        letter2CodeMap.put("d", "100");
        letter2CodeMap.put("e", "101");
        letter2CodeMap.put("f", "102");
        letter2CodeMap.put("g", "103");
        letter2CodeMap.put("h", "104");
        letter2CodeMap.put("i", "105");
        letter2CodeMap.put("j", "106");
        letter2CodeMap.put("k", "107");
        letter2CodeMap.put("l", "108");
        letter2CodeMap.put("m", "109");
        letter2CodeMap.put("n", "110");
        letter2CodeMap.put("o", "111");
        letter2CodeMap.put("p", "112");
        letter2CodeMap.put("q", "113");
        letter2CodeMap.put("r", "114");
        letter2CodeMap.put("s", "115");
        letter2CodeMap.put("t", "116");
        letter2CodeMap.put("u", "117");
        letter2CodeMap.put("v", "118");
        letter2CodeMap.put("w", "119");
        letter2CodeMap.put("x", "120");
        letter2CodeMap.put("y", "121");
        letter2CodeMap.put("z", "122");
    }

    private Context mContext;               //上下文

    private final LinearLayout keyboardOuterContainer;
    private View keyContainer;              //自定义键盘的容器View
    private SafeKeyboardView keyboardView;  //键盘的View
    private ImageView keyboardBgImg;        //键盘背景图片
    private ImageView keyboardImg;          //键盘顶部中间显示的图片
    private ImageView keyboardDoneImg;      //键盘顶部右侧隐藏键盘的图片按键
    private FrameLayout keyboardDoneImgLayout;//键盘顶部右侧隐藏键盘的图片按键容器
    private TextView keyboardTip;           //键盘顶部中间显示的文字
    private Keyboard keyboardNumber;        //数字键盘, 包含了基本的 + - * / . 等符号
    private Keyboard keyboardNumberRandom;  //数字键盘, 包含了基本的 + - * / . 等符号  随机
    private Keyboard keyboardNumberOnly;    //纯数字键盘, 只有数字
    private Keyboard keyboardNumberOnlyRandom;//纯数字键盘, 只有数字  随机
    private Keyboard keyboardSymbol;        //符号键盘
    private Keyboard keyboardIdCard;        //中国身份证号码键盘
    private Keyboard keyboardIdCardRandom;  //中国身份证号码键盘  随机
    private Keyboard keyboardLetter;        //字母键盘总成, keyboardLetterOnly 和 keyboardLetterNum 初始化后, 赋值给此变量
    private Keyboard keyboardLetterRandom;  //随机字母键盘总成, keyboardLetterOnly 和 keyboardLetterNum 初始化后, 赋值给此变量
    private boolean isJustChangeLetterCase = false;
    private boolean changeLetCaseRefreshRandom = false;
    private static boolean isCapes = false;
    private boolean isCapLock = false;
    private boolean isShowStart = false;
    private boolean isHideStart = false;
    private boolean enablePreview = false;  // 关闭按键预览功能, 默认关闭按键点击预览功能, 需要时在初始化时开启即可
    private boolean isVibrateEnable = false;
    private int keyboardType = 1;           // SafeKeyboard 键盘类型
    // 下面这个不用了, 先注释, 暂不删除
    // private int mCurrentInputTypeInEdit;    // 当前 EditText 的输入类型, (其实这个参数比较鸡肋, 使用 mCurrentEditText 即可)
    private final Handler safeHandler = new Handler(Looper.getMainLooper());

    /**
     * 只起到延时开始显示的作用
     */
    private final Runnable showRun = () -> showKeyboard(true);

    private final Runnable hideRun = this::hideKeyboard;

    private final Runnable hideEnd = this::doHideEnd;

    private final Runnable showEnd = this::doShowEnd;

    private TranslateAnimation showAnimation;
    private TranslateAnimation hideAnimation;
    private long lastTouchTime;
    private EditText mCurrentEditText;
    private SparseArray<Keyboard.Key> randomDigitKeys;
    private SparseArray<Keyboard.Key> randomDigitNumOnlyKeys;
    private SparseArray<Keyboard.Key> randomIdCardDigitKeys;
    private Keyboard.Key curPressKey;
    private boolean enableCancelInput;
    private boolean isInputCancel;

    /**
     * 默认都是小写字符
     */
    private SparseArray<Keyboard.Key> randomLetterKeys;
    private HashMap<String, Integer> mEditLastKeyboardTypeMap;

    private Set<String> mEditTagSet;
    private Set<String> mIdCardEditTagSet;
    private Set<String> mRandomEditTagSet;
    // 字符、字母、数字三个随机键盘切换时, 字母按键始终刷新记录集合
    private Set<String> mRandomSybLetKbdSwitchRefreshEditTagSet;
    // 字符、字母、数字三个随机键盘切换时, 数字按键始终刷新记录集合
    private Set<String> mRndNumKbdSwitchAlwaysRefreshEditTagSet;
    private Set<String> mVibrateEditTagSet;
    private HashMap<String, KeyBgResEntitySet> mCusKeyBgMap;

    private View.OnTouchListener onEditTextTouchListener;
    private final View rootView;
    private final View mScrollLayout;
    private ViewTreeObserver.OnGlobalFocusChangeListener onGlobalFocusChangeListener;
    private ViewTreeObserver treeObserver;
    private ViewPoint downPoint;
    private ViewPoint upPoint;
    private int mScreenWidth;
    private int mScreenHeight;
    private float toBackSize;   // 往上移动的距离, 为负值
    private int[] originalScrollPosInScr;
    private int[] originalScrollPosInPar;

    private Vibrator mVibrator;

    private boolean isWebViewUsage = false;

    // 已支持多 EditText 共用一个 SafeKeyboard

    /**
     * SafeKeyboard 构造方法, 传入必要的参数, 已精简传入参数
     * 使用 SafeKeyboard 布局为默认布局 layout_keyboard_container
     *
     * @param mContext               上下文 Context
     * @param keyboardOuterContainer 使用 SafeKeyboard 的界面上显示 SafeKeyboard 的 容器 View, 这里写死只能是 LinearLayout
     * @param rootView               含有使用了 SafeKeyboard 的 EditText 的界面根布局 View
     *                               传入目的是为了获取 rootView 下所有的 EditText 以便对焦点事件进行监测和处理
     * @param scrollLayout           目标 EditText 父布局 View
     *                               ( 多个 EditText 共用 SafeKeyboard 但其直接父布局不是同一个 View 时, 传入公共父布局)
     *                               传入目的是：当 EditText 需要被顶起的时候, 顶起该布局, 以达到输入时可以显示已输入内容的功能
     *                               注意, 可以是 EditText 本身, 不过需要传入 View 类型的 EditText
     */
    public SafeKeyboard(Context mContext, LinearLayout keyboardOuterContainer, @NonNull View rootView, @NonNull View scrollLayout) {
        this(mContext, keyboardOuterContainer, rootView, scrollLayout, null);
    }

    /**
     * SafeKeyboard 构造方法, 传入必要的参数
     * <p>仅支持 WebView 中使用时调用</p>
     * <p>其他时候调用可能会崩溃, 慎用</p>
     *
     * @param mContext       上下文 Context
     * @param keyboardConfig 配置文件
     */
    public SafeKeyboard(Context mContext, LinearLayout keyboardOuterContainer, SafeKeyboardConfig keyboardConfig) {
        this.mContext = mContext;
        this.keyboardConfig = keyboardConfig == null ? SafeKeyboardConfig.getDefaultConfig() : keyboardConfig;
        // this.keyboardConfig.letterWithNumber = false;   // 暂不支持带数字的字母键盘
        this.keyboardOuterContainer = keyboardOuterContainer;
        this.rootView = null;
        this.mScrollLayout = null;
        this.isWebViewUsage = true;

        initData();
        initKeyboardAndFindView();
        initKeyboardConfig();
        setListeners();
        initAnimation();
    }

    /**
     * SafeKeyboard 构造方法, 传入必要的参数, 已精简传入参数
     * 使用 SafeKeyboard 布局为默认布局 layout_keyboard_container
     *
     * @param mContext               上下文 Context
     * @param keyboardOuterContainer 使用 SafeKeyboard 的界面上显示 SafeKeyboard 的 容器 View, 这里写死只能是 LinearLayout
     * @param rootView               含有使用了 SafeKeyboard 的 EditText 的界面根布局 View
     *                               传入目的是为了获取 rootView 下所有的 EditText 以便对焦点事件进行监测和处理
     * @param scrollLayout           目标 EditText 父布局 View
     *                               ( 多个 EditText 共用 SafeKeyboard 但其直接父布局不是同一个 View 时, 传入公共父布局)
     *                               传入目的是：当 EditText 需要被顶起的时候, 顶起该布局, 以达到输入时可以显示已输入内容的功能
     *                               注意, 可以是 EditText 本身, 不过需要传入 View 类型的 EditText
     */
    public SafeKeyboard(Context mContext, LinearLayout keyboardOuterContainer, @NonNull View rootView, @NonNull View scrollLayout, SafeKeyboardConfig keyboardConfig) {
        this.mContext = mContext;
        this.keyboardConfig = keyboardConfig == null ? SafeKeyboardConfig.getDefaultConfig() : keyboardConfig;
        // this.keyboardConfig.letterWithNumber = false;   // 暂不支持带数字的字母键盘
        this.keyboardOuterContainer = keyboardOuterContainer;
        this.rootView = rootView;
        this.mScrollLayout = scrollLayout;

        initData();
        initKeyboardAndFindView();
        initKeyboardConfig();
        setListeners();
        initAnimation();
    }

    public void enableRememberLastKeyboardType() {
        keyboardView.setRememberLastType(true);
    }

    public void disableRememberLastKeyboardType() {
        keyboardView.setRememberLastType(false);
    }

    public void enableChangeLetCaseRefreshRandom() {
        changeLetCaseRefreshRandom = true;
    }

    public void disableChangeLetCaseRefreshRandom() {
        changeLetCaseRefreshRandom = false;
    }

    private void initData() {
        isCapLock = false;
        isCapes = false;
        isVibrateEnable = false;
        toBackSize = 0;
        downPoint = new ViewPoint();
        upPoint = new ViewPoint();
        mEditTagSet = new HashSet<>();
        mIdCardEditTagSet = new HashSet<>();
        mRandomEditTagSet = new HashSet<>();
        mRandomSybLetKbdSwitchRefreshEditTagSet = new HashSet<>();
        mRndNumKbdSwitchAlwaysRefreshEditTagSet = new HashSet<>();
        mVibrateEditTagSet = new HashSet<>();
        mEditLastKeyboardTypeMap = new HashMap<>();
        mCusKeyBgMap = new HashMap<>();
        mVibrator = null;
        originalScrollPosInScr = new int[]{0, 0, 0, 0};
        originalScrollPosInPar = new int[]{0, 0, 0, 0};
        lastTouchTime = 0L;

        curPressKey = null;
        isInputCancel = false;

        // 获取 WindowManager 实例, 得到屏幕的操作权
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            // 给 metrics 赋值
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);
            // 设备屏幕的宽度,高度变量
            mScreenWidth = metrics.widthPixels;
            mScreenHeight = metrics.heightPixels;
        }
    }

    private void initKeyboardAndFindView() {
        keyContainer = LayoutInflater.from(mContext).inflate(keyboardConfig.keyboardContainerLayoutId, keyboardOuterContainer, true);
        keyboardView = keyContainer.findViewById(keyboardConfig.safeKeyboardViewId);
        keyboardBgImg = keyContainer.findViewById(R.id.keyboardBgImg);
        keyboardImg = keyContainer.findViewById(R.id.keyboardImg);
        keyboardTip = keyContainer.findViewById(R.id.keyboardTip);
        keyboardDoneImg = keyContainer.findViewById(R.id.keyboardDoneImg);
        keyboardDoneImgLayout = keyContainer.findViewById(R.id.keyboardDone);
        keyContainer.setVisibility(View.GONE);

        // 各种键盘初始化
        keyboardNumber = new Keyboard(mContext, R.xml.keyboard_num_symbol);     //实例化数字键盘
        keyboardNumberRandom = new Keyboard(mContext, R.xml.keyboard_num_symbol);     //实例化数字键盘  随机
        // 注: 这里有三个数字键盘,  keyboard_num_symbol:带部分符号;   keyboard_num:可切换的数字键盘;    keyboard_num_only:纯数字键盘, 不可切换
        keyboardNumberOnly = new Keyboard(mContext, R.xml.keyboard_num_only);
        keyboardNumberOnlyRandom = new Keyboard(mContext, R.xml.keyboard_num_only);

        //字母键盘, 只包含字母, 没有数字
        Keyboard keyboardLetterOnly = new Keyboard(mContext, R.xml.keyboard_letter);            //实例化字母键盘
        Keyboard keyboardLetterOnlyRandom = new Keyboard(mContext, R.xml.keyboard_letter);      //实例化字母键盘 随机
        //字母数字键盘, 第一行为数字
        Keyboard keyboardLetterNum = new Keyboard(mContext, R.xml.keyboard_letter_num);         //实例化字母键盘
        Keyboard keyboardLetterNumRandom = new Keyboard(mContext, R.xml.keyboard_letter_num);   //实例化字母键盘 随机
        keyboardSymbol = new Keyboard(mContext, R.xml.keyboard_symbol);         //实例化符号键盘
        keyboardIdCard = new Keyboard(mContext, R.xml.keyboard_id_card_zn);     //实例化 IdCard(中国身份证) 键盘
        keyboardIdCardRandom = new Keyboard(mContext, R.xml.keyboard_id_card_zn);     //实例化 IdCard(中国身份证) 键盘  随机
        // 由于符号键盘与字母键盘共用一个KeyBoardView, 所以不需要再为符号键盘单独实例化一个KeyBoardView

        keyboardLetter = keyboardConfig.letterWithNumber ? keyboardLetterNum : keyboardLetterOnly;
        keyboardLetterRandom = keyboardConfig.letterWithNumber ? keyboardLetterNumRandom : keyboardLetterOnlyRandom;

        // 后面在考虑是否这样用
        // changeLetCaseRefreshRandom = keyboardConfig.changeLetCaseRefreshRandom;

        initNumOnlyKeyboardKeyNoneTitle();
        // 随机键盘
        initRandomDigitKeys();
        initRandomDigitNumOnlyKeys();
        initIdCardRandomDigitKeys();
        initRandomLetterKeys();
    }

    private void initKeyboardConfig() {
        keyboardBgImg.setImageResource(keyboardConfig.keyboardBgResId);
        keyboardBgImg.setScaleType(keyboardConfig.keyboardBgScaleType);
        keyboardImg.setImageResource(keyboardConfig.keyboardShieldImgResId);
        keyboardTip.setText(keyboardConfig.keyboardTitle);
        keyboardTip.setTextColor(mContext.getResources().getColor(keyboardConfig.keyboardTitleColor));
        keyboardDoneImg.setImageResource(keyboardConfig.keyboardDoneImgResId);
        keyboardDoneImgLayout.setBackgroundResource(keyboardConfig.keyboardDoneImgLayoutResId);
        keyboardView.setDelDrawable(ContextCompat.getDrawable(mContext, keyboardConfig.iconResIdDel));
        keyboardView.setLowDrawable(ContextCompat.getDrawable(mContext, keyboardConfig.iconResIdLowLetter));
        keyboardView.setUpDrawable(ContextCompat.getDrawable(mContext, keyboardConfig.iconResIdUpLetter));
        keyboardView.setUpDrawableLock(ContextCompat.getDrawable(mContext, keyboardConfig.iconResIdUpLetterLock));
        keyboardView.setSpecialKeyBgResId(keyboardConfig.keyboardSpecialKeyBgResId);
        keyboardView.setKeyTextSize(keyboardConfig.keyboardKeyTextSize);
        keyboardView.setKeyLabelSize(keyboardConfig.keyboardKeyLabelSize);
        keyboardView.setNormalKeyBgResId(keyboardConfig.keyboardNormalKeyBgResId);
        keyboardView.setNormalKeyTextColorId(keyboardConfig.keyboardNormalKeyColorId);
        keyboardView.setEnabled(true);
        keyboardView.setPreviewEnabled(false);

        enableCancelInput = keyboardConfig.enableCancelInput;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setListeners() {
        keyboardView.setOnKeyboardActionListener(listener);

        FrameLayout done = keyContainer.findViewById(R.id.keyboardDone);
        done.setOnClickListener(v -> {
            Log.d(TAG, "*************** hide key clicked");
            if (isKeyboardShown()) {
                safeHandler.removeCallbacks(hideRun);
                safeHandler.removeCallbacks(showRun);
                safeHandler.postDelayed(hideRun, keyboardConfig.hideDelay);
            }
        });

        keyboardView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                // Log.w(TAG, "ACTION_MOVE");
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Log.w(TAG, "ACTION_DOWN");
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                // Log.w(TAG, "ACTION_UP");
                int mUpX = (int) event.getX();
                int mUpY = (int) event.getY();
                if (curPressKey != null && !curPressKey.isInside(mUpX, mUpY) && enableCancelInput) {
                    // Log.e(TAG, "抬起手指的位置位于 Key 之外, key: " + curPressKey.label);
                    isInputCancel = true;
                }
            }

            return false;
        });

        if (rootView != null) {
            treeObserver = rootView.getViewTreeObserver();
            onGlobalFocusChangeListener = (oldFocus, newFocus) -> {
                if (newFocus instanceof EditText) {
                    EditText newEdit = (EditText) newFocus;
                    if (isEditSetContainThisEdit(newEdit)) {
                        hideSystemKeyBoard(newEdit);
                    }
                    // forceChangeEditImeOptNextToDone(newEdit);
                }
                if (oldFocus instanceof EditText) {
                    // 上一个获得焦点的为 EditText
                    EditText oldEdit = (EditText) oldFocus;
                    if (isEditSetContainThisEdit(oldEdit)) {
                        // 前 EditText 使用了 SafeKeyboard
                        // 新获取焦点的是 EditText
                        if (newFocus instanceof EditText) {
                            EditText newEdit = (EditText) newFocus;
                            if (isEditSetContainThisEdit(newEdit)) {
                                // 该 EditText 也使用了 SafeKeyboard
                                // Log.i(TAG, "Safe --> Safe, 开始检查是否需要手动 show");
                                keyboardPreShow(newEdit);
                            } else {
                                // 该 EditText 没有使用 SafeKeyboard, 则隐藏 SafeKeyboard
                                // Log.i(TAG, "Safe --> 系统, 开始检查是否需要手动 hide");

                                // 说明: 如果 EditText 外被 ScrollView 包裹, 切换成系统输入法的时候, SafeKeyboard 会被异常顶起
                                // 需要在 Activity 的声明中增加 android:windowSoftInputMode="stateAlwaysHidden|adjustPan" 语句
                                keyboardPreHide();
                            }
                        } else {
                            // 新获取焦点的不是 EditText, 则隐藏 SafeKeyboard
                            // Log.i(TAG, "Safe --> 其他, 开始检查是否需要手动 hide");
                            keyboardPreHide();
                        }
                    } else {
                        // 前 EditText 没有使用 SafeKeyboard
                        // 新获取焦点的是 EditText
                        if (newFocus instanceof EditText) {
                            EditText newEdit = (EditText) newFocus;
                            // 该 EditText 使用了 SafeKeyboard, 则显示
                            if (isEditSetContainThisEdit(newEdit)) {
                                // Log.i(TAG, "系统 --> Safe, 开始检查是否需要手动 show");
                                keyboardPreShow(newEdit);
                            } else {
                                // Log.i(TAG, "系统 --> 系统, 开始检查是否需要手动 hide");
                                keyboardPreHide();
                            }
                        } else {
                            // ... 否则不需要管理此次事件, 但是为保险起见, 可以隐藏一次 SafeKeyboard, 当然隐藏前需要判断是否已显示
                            // Log.i(TAG, "系统 --> 其他, 开始检查是否需要手动 hide");
                            keyboardPreHide();
                        }
                    }
                } else {
                    // 新获取焦点的是 EditText
                    if (newFocus instanceof EditText) {
                        EditText newEdit = (EditText) newFocus;
                        // 该 EditText 使用了 SafeKeyboard, 则显示
                        if (isEditSetContainThisEdit(newEdit)) {
                            // Log.i(TAG, "其他 --> Safe, 开始检查是否需要手动 show");
                            keyboardPreShow(newEdit);
                        } else {
                            // Log.i(TAG, "其他 --> 系统, 开始检查是否需要手动 hide");
                            keyboardPreHide();
                        }
                    } else {
                        // ... 否则不需要管理此次事件, 但是为保险起见, 可以隐藏一次 SafeKeyboard, 当然隐藏前需要判断是否已显示
                        // Log.i(TAG, "其他 --> 其他, 开始检查是否需要手动 hide");
                        keyboardPreHide();
                    }
                }
            };
            treeObserver.addOnGlobalFocusChangeListener(onGlobalFocusChangeListener);
        } else {
            Log.e(TAG, "Root View is null!");
            // throw new Exception("Root View is null");
        }

        // 这里应该只处理一个事件, 就是:
        //    当 SafeKeyboard 已经被隐藏了, 再次点击当前 EditText 时, 重新显示.
        //    其他的显示、切换、点击其他 EditText 等操作应该由别的逻辑处理. 比如: OnGlobalFocusChangeListener 中处理等
        //    当 SafeKeyboard 从未显示时, 不应该在这里处理, 所以这里要判断 mCurrentEditText 不为空时, 才处理
        //    其实这里的多个判断逻辑已经基本上规避了 mCurrentEditText 为空的情况, 这里就保留一下
        onEditTextTouchListener = (v, event) -> {
            if (v instanceof EditText) {
                EditText mEditText = (EditText) v;
                hideSystemKeyBoard(mEditText);
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downPoint.setCoo_x((int) event.getRawX());
                    downPoint.setCoo_y((int) event.getRawY());
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    upPoint.setCoo_x((int) event.getRawX());
                    upPoint.setCoo_y((int) event.getRawY());
                    // 这里 mEditText.hasFocus(), 是判断符合 SafeKeyboard 被隐藏后再次点击当前 EditText 条件的关键
                    // 因为隐藏 SafeKeyboard 后, 一般来说焦点还是在这个 EditText 中的,
                    // 如果焦点丢失, 再次点击就会触发焦点变化, 那么就属于焦点变化处理逻辑, 而不是在这里处理.
                    // 所以, 加上 mEditText.hasFocus() 判断点击的是已有焦点的 EditText 且 SafeKeyboard 被隐藏了, 需要重新显示
                    if (isTouchConsiderClick(downPoint, upPoint, mEditText) && mEditText.hasFocus() && !isShow()
                            && mCurrentEditText != null && mCurrentEditText == mEditText) {
                        // 点击了当前 EditText, 且 SafeKeyboard 未显示, 那么重新显示 SafeKeyboard
                        keyboardPreShow(mEditText);
                    }
                    downPoint.clearPoint();
                    upPoint.clearPoint();
                }
            }
            return false;
        };
    }

    // 不用强制改为 完成, 这样会限制客户的其他需求, 应该判断新的 EditText 是否使用了 SafeKeyboard
    // 使用了的话, 直接隐藏系统软件盘就可以了
    private void forceChangeEditImeOptNextToDone(EditText newEdit) {
        // 强制非使用 SafeKeyboard 的 EditText 且下一项、完成、搜索等被设置为 下一项的 设置为完成,
        // 否则可能会导致点下一项时, SafeKeyboard 和 系统键盘 同时出现
        // Log.w(TAG, "ime: " + newEdit.getImeOptions());
        if (!isEditSetContainThisEdit(newEdit) && (newEdit.getImeOptions() == EditorInfo.IME_ACTION_NEXT || newEdit.getImeOptions() == EditorInfo.IME_ACTION_UNSPECIFIED || newEdit.getImeOptions() == EditorInfo.IME_ACTION_NONE)) {
            newEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
            Object tagObj = newEdit.getTag();
            if (tagObj != null) {
                // 其实这里一般不会进入, 除非调用者 主动设置了 tag
                Log.w(TAG, "Tag 为: " + newEdit.getTag().toString() + " 的 EditText 未使用 SafeKeyboard," + " ImeOptions 属性为 IME_ACTION_NEXT-IME_ACTION_UNSPECIFIED-IME_ACTION_NONE 三者之一, " + "为避免软键盘显示出错, 现强制设置为 IME_ACTION_DONE--完成");
            } else {
                String msg = "这个 EditText 未使用 SafeKeyboard, 因此未设置 Tag! 此 EditText id 为: " + newEdit.getId() + ", ImeOptions 属性为 IME_ACTION_NEXT-" + "IME_ACTION_UNSPECIFIED-IME_ACTION_NONE 三者之一, 为避免软键盘显示出错, 现强制设置为 IME_ACTION_DONE--完成";
                Log.w(TAG, msg);
            }
        }
    }

    private void initAnimation() {
        showAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        hideAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
        showAnimation.setDuration(keyboardConfig.showDuration);
        hideAnimation.setDuration(keyboardConfig.hideDuration);

        showAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                Log.d(TAG, "*************** startAnimation start");
                isShowStart = true;
                // 在这里设置可见, 会出现第一次显示键盘时直接闪现出来, 没有动画效果, 后面正常
                // keyContainer.setVisibility(View.VISIBLE);
                // 动画持续时间 SHOW_TIME 结束后, 不管什么操作, 都需要执行, 把 isShowStart 值设为 false; 否则
                // 如果 onAnimationEnd 因为某些原因没有执行, 会影响下一次使用
                safeHandler.removeCallbacks(showEnd);
                safeHandler.postDelayed(showEnd, keyboardConfig.showDuration);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Log.d(TAG, "*************** startAnimation end");
                isShowStart = false;
                keyContainer.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        hideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                Log.d(TAG, "*************** hideAnimation start");
                isHideStart = true;
                // 动画持续时间 HIDE_TIME 结束后, 不管什么操作, 都需要执行, 把 isHideStart 值设为 false; 否则
                // 如果 onAnimationEnd 因为某些原因没有执行, 会影响下一次使用
                safeHandler.removeCallbacks(hideEnd);
                safeHandler.postDelayed(hideEnd, keyboardConfig.hideDuration);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Log.d(TAG, "*************** hideAnimation end");
                safeHandler.removeCallbacks(hideEnd);
                if (isHideStart) {
                    // isHideStart 未被置为初试状态, 说明还没有执行 hideEnd 内容, 这里手动执行一下
                    doHideEnd();
                }
                // 说明已经被执行了不需要在执行一遍了, 下面就什么都不用管了
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    /**
     * 用来计算按下和抬起时的两点位置的关系, 是否可以将此次 Touch 事件 看作 Click 事件
     * 两点各自的 x/y 轴距离不超过 10, 且两点中心点在目标 EditText 上 时, 返回 true, 否则 false
     *
     * @param down      按下时的位置点
     * @param up        抬起时的位置点
     * @param mEditText 目标 EditText
     * @return 是否考虑此次为点击事件
     */
    private boolean isTouchConsiderClick(ViewPoint down, ViewPoint up, EditText mEditText) {
        boolean flag = false;
        if (Math.abs(down.getCoo_x() - up.getCoo_x()) < 10 && Math.abs(down.getCoo_y() - up.getCoo_y()) < 10) {
            int[] position = new int[2];
            mEditText.getLocationOnScreen(position);
            int width = mEditText.getWidth();
            int height = mEditText.getHeight();
            int x = (down.getCoo_x() + up.getCoo_x()) / 2;
            int y = (down.getCoo_y() + up.getCoo_y()) / 2;
            if (position[0] + width >= x && position[1] + height >= y) flag = true;
        }

        return flag;
    }

    private void keyboardPreHide() {
        safeHandler.removeCallbacks(hideRun);
        safeHandler.removeCallbacks(showRun);
        getOriginalScrollLayoutPos();
        if (stillNeedOptManually(false)) {
            safeHandler.postDelayed(hideRun, keyboardConfig.hideDelay);
        }
    }

    private void keyboardPreShow(final EditText mEditText) {
        safeHandler.removeCallbacks(showRun);
        safeHandler.removeCallbacks(hideRun);
        getOriginalScrollLayoutPos();
        if (stillNeedOptManually(true)) {
            setCurrentEditText(mEditText);
            safeHandler.postDelayed(showRun, keyboardConfig.showDelay);
        } else {
            // 说明不需要再手动显示, 只需要切换键盘模式即可 (甚至不用切换)
            // 这里需要检查当前 EditText 的显示是否合理
            final long delay = doScrollLayoutBack(false, mEditText) ? keyboardConfig.hideDuration + 50 : 0;
            new Handler().postDelayed(() -> {
                // 如果已经显示了, 那么切换键盘即可
                setCurrentEditText(mEditText);
                showKeyboard(false);
            }, delay);
        }
    }

    private void initNumOnlyKeyboardKeyNoneTitle() {
        List<Keyboard.Key> keys = keyboardNumberOnly.getKeys();
        for (Keyboard.Key key : keys) {
            if (key.codes[0] == 100861) {
                key.label = keyboardConfig.keyboardNumOnlyKeyNoneTitle;
                break;
            }
        }
    }

    private void initRandomDigitKeys() {
        randomDigitKeys = new SparseArray<>();
        List<Keyboard.Key> keys = keyboardNumberRandom.getKeys();
        for (Keyboard.Key key : keys) {
            int code = key.codes[0];
            if (code >= 48 && code <= 57) randomDigitKeys.put(code, key);
        }
        refreshDigitKeyboard(keyboardNumberRandom);
    }

    private void initRandomDigitNumOnlyKeys() {
        randomDigitNumOnlyKeys = new SparseArray<>();
        List<Keyboard.Key> keys = keyboardNumberOnlyRandom.getKeys();
        for (Keyboard.Key key : keys) {
            int code = key.codes[0];
            if (code >= 48 && code <= 57) randomDigitNumOnlyKeys.put(code, key);
            if (code == 100861) {
                key.label = keyboardConfig.keyboardNumOnlyKeyNoneTitle;
            }
        }
        refreshDigitKeyboard(keyboardNumberOnlyRandom);
    }

    /**
     * 中国身份证键盘--随机, 只随机数字按键
     */
    private void initIdCardRandomDigitKeys() {
        randomIdCardDigitKeys = new SparseArray<>();
        List<Keyboard.Key> keys = keyboardIdCardRandom.getKeys();
        for (Keyboard.Key key : keys) {
            int code = key.codes[0];
            if (code >= 48 && code <= 57) randomIdCardDigitKeys.put(code, key);
        }
        refreshDigitKeyboard(keyboardIdCardRandom);
    }

    private void initRandomLetterKeys() {
        randomLetterKeys = new SparseArray<>();
        List<Keyboard.Key> keys = keyboardLetterRandom.getKeys();
        for (Keyboard.Key key : keys) {
            int code = key.codes[0];
            if (code >= 97 && code <= 122) randomLetterKeys.put(code, key);
        }
        refreshRandomLetterKeyboard(keyboardLetterRandom);
    }

    /**
     * 更新 mScrollLayout 原始位置, 且只获取一次
     */
    private void getOriginalScrollLayoutPos() {
        if (originalScrollPosInScr[0] == 0 && originalScrollPosInScr[1] == 0) {
            int[] pos = new int[]{0, 0};
            mScrollLayout.getLocationOnScreen(pos);
            originalScrollPosInScr[0] = pos[0];
            originalScrollPosInScr[1] = pos[1];
            originalScrollPosInScr[2] = pos[0] + mScrollLayout.getWidth();
            originalScrollPosInScr[3] = pos[1] + mScrollLayout.getHeight();
        }

        if (originalScrollPosInPar[0] == 0 && originalScrollPosInPar[1] == 0 && originalScrollPosInPar[2] == 0 && originalScrollPosInPar[3] == 0) {
            originalScrollPosInPar[0] = mScrollLayout.getLeft();
            originalScrollPosInPar[1] = mScrollLayout.getTop();
            originalScrollPosInPar[2] = mScrollLayout.getRight();
            originalScrollPosInPar[3] = mScrollLayout.getBottom();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void putEditText(EditText mEditText) {
        if (mEditTagSet == null) mEditTagSet = new HashSet<>();
        String tag = Integer.toHexString(System.identityHashCode(mEditText));
        mEditText.setTag(tag);
        if (!mEditTagSet.contains(tag)) {
            mEditTagSet.add(mEditText.getTag().toString());
            mEditText.setOnTouchListener(onEditTextTouchListener);
        }
    }

    public void putEditText2IdCardType(EditText mEditText) {
        putEditText(mEditText);
        if (mIdCardEditTagSet == null) mIdCardEditTagSet = new HashSet<>();
        mIdCardEditTagSet.add(mEditText.getTag().toString());
    }

    public void putVibrateEdit(EditText mEditText) {
        if (isVibrateEnable) {
            // 只有开启了, 才会震动
            keyboardView.enableVibrate();
        }
        putEditText(mEditText);
        if (mVibrateEditTagSet == null) mVibrateEditTagSet = new HashSet<>();
        if (mVibrateEditTagSet.contains(mEditText.getTag().toString())) {
            Log.w(TAG, "This edit has been set to vibrate already!!!");
            return;
        }
        mVibrateEditTagSet.add(mEditText.getTag().toString());
    }

    public void putRandomEdit(EditText mEditText) {
        // 默认开启字母、数字来回切换时刷新随机显示位置, 否则一直不刷新
        putRandomEdit(mEditText, true, true);
    }

    /**
     * @param mEditText           目标 Edit
     * @param switchRefreshLetter 切换到期其他输入模式后再切换回字母输入时是否刷新字母显示位置
     * @param switchRefreshNum    切换到期其他输入模式后再切换回数字输入时是否刷新数字显示位置
     */
    public void putRandomEdit(EditText mEditText, boolean switchRefreshLetter, boolean switchRefreshNum) {
        putEditText(mEditText);
        putRandomEditTextTag(mEditText, switchRefreshLetter, switchRefreshNum);
    }

    /**
     * @param mEditText           要设置的 EditText
     * @param switchRefreshLetter 切换到期其他输入模式后再切换回字母输入时是否刷新字母显示位置
     * @param switchRefreshNum    切换到期其他输入模式后再切换回数字输入时是否刷新数字显示位置
     */
    private void putRandomEditTextTag(EditText mEditText, boolean switchRefreshLetter, boolean switchRefreshNum) {
        if (mRandomEditTagSet == null) mRandomEditTagSet = new HashSet<>();
        if (mRandomEditTagSet.contains(mEditText.getTag().toString())) {
            Log.w(TAG, "This edit has been set to random already!!!");
        } else {
            mRandomEditTagSet.add(mEditText.getTag().toString());
        }

        // 设置在同一个 EditText 中切换字符、字母、数字键盘时, 重新设置随机字母的显示位置
        if (switchRefreshLetter) {
            if (mRandomSybLetKbdSwitchRefreshEditTagSet == null)
                mRandomSybLetKbdSwitchRefreshEditTagSet = new HashSet<>();
            if (mRandomSybLetKbdSwitchRefreshEditTagSet.contains(mEditText.getTag().toString())) {
                Log.w(TAG, "This edit has been set to random keyboard always refresh already!!!");
            } else {
                mRandomSybLetKbdSwitchRefreshEditTagSet.add(mEditText.getTag().toString());
            }
        }
        // 设置在同一个 EditText 中切换字符、字母、数字键盘时, 重新设置随机数字的显示位置
        if (switchRefreshNum) {
            if (mRndNumKbdSwitchAlwaysRefreshEditTagSet == null)
                mRndNumKbdSwitchAlwaysRefreshEditTagSet = new HashSet<>();
            if (mRndNumKbdSwitchAlwaysRefreshEditTagSet.contains(mEditText.getTag().toString())) {
                Log.w(TAG, "This edit has been set to random keyboard always refresh already!!!");
            } else {
                mRndNumKbdSwitchAlwaysRefreshEditTagSet.add(mEditText.getTag().toString());
            }
        }
    }

    /**
     * 设置是否开启预览功能, 默认关闭
     * <p>
     * 解释：因为系统自带的 KeyboardView 的按键预览功能是使用 PopupWindow 来实现的, 那么在
     * PopupWindow 中使用了 SafeKeyboard (本软键盘), 那么必须关闭预览, 否则会直接崩溃.
     * ( 即调用 setForbidPreview(true) )
     *
     * @param enablePreview 是否开启预览
     */
    public void setEnablePreview(boolean enablePreview) {
        this.enablePreview = enablePreview;
    }

    public void setEditKeyBgResArray(EditText mEditText, KeyBgResEntitySet entitySet) {
        if (mCusKeyBgMap == null) mCusKeyBgMap = new HashMap<>();
        if (mEditText.getTag() == null) {
            Log.w(TAG, "设置了自定义按键背景样式, 但是对应的 EditText 未设置 tag, 请检查调用顺序后重试!");
            return;
        }
        if (!mCusKeyBgMap.containsKey(mEditText.getTag().toString())) {
            mCusKeyBgMap.put(mEditText.getTag().toString(), entitySet);
        }
    }

    // 设置键盘点击监听
    private final KeyboardView.OnKeyboardActionListener listener = new KeyboardView.OnKeyboardActionListener() {

        @Override
        public void onPress(int primaryCode) {
            if (primaryCode == -1 || primaryCode == -5 || primaryCode == 32 || primaryCode == -2 || primaryCode == 100860 || primaryCode == 100861 || primaryCode == -35) {
                keyboardView.setPreviewEnabled(false); // 特殊按键不支持预览
            }
            // Log.e(TAG, "onPress, " + primaryCode);
            // 这里只是获取当前, 不对键盘相关参数进行更新
            Keyboard keyboard = getKeyboardByCurKeyboardType();
            List<Keyboard.Key> keys = keyboard.getKeys();
            if (keys == null) {
                Log.e(TAG, "Keyboard keys is null!");
                return;
            }
            for (Keyboard.Key key : keys) {
                if (key != null && key.codes[0] == primaryCode) {
                    curPressKey = key;
                    return;
                }
            }

            /*if (keyboardType == 3 || keyboardType == 4 || keyboardType == 5) {
                // 数字键盘
                keyboardView.setPreviewEnabled(enablePreview);
            } else {
                keyboardView.setPreviewEnabled(enablePreview);
                if (primaryCode == -1 || primaryCode == -5 || primaryCode == 32 || primaryCode == -2 || primaryCode == 100860 || primaryCode == 100861 || primaryCode == -35) {
                    keyboardView.setPreviewEnabled(false); // 特殊按键不支持预览
                }
                // else {
                //     keyboardView.setPreviewEnabled(!forbidPreview);
                // }
            }*/
        }

        @Override
        public void onRelease(int primaryCode) {
            curPressKey = null;
            isInputCancel = false;

            // Log.e(TAG, "onRelease, " + primaryCode);
        }

        @Override
        public void onKey(int primaryCode, int[] keyCodes) {
            if (isInputCancel) {
                // Log.e(TAG, "取消输入, 抬起手指的位置位于 Key 之外, key: " + curPressKey.label);
                isInputCancel = false;
                return;
            }
            try {
                if (primaryCode == Keyboard.KEYCODE_CANCEL) {
                    // 隐藏键盘
                    safeHandler.removeCallbacks(hideRun);
                    safeHandler.removeCallbacks(showRun);
                    safeHandler.post(hideRun/*, HIDE_DELAY*/);
                } else if (primaryCode == Keyboard.KEYCODE_DELETE || primaryCode == -35) {
                    // 回退键,删除字符
                    onKeyboardDeletePressed();
                } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
                    // 大小写切换
                    onKeyboardLetterCaseChangePressed();
                } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
                    // 数字与字母键盘互换
                    onKeyboardModeChangePressed();
                } else if (primaryCode == 100860) {
                    // 字母与符号切换
                    onKeyboardSybLetSwitchPressed();
                } else if (primaryCode == 100861) {
                    // TODO... 这里暂时啥也不干, 占位用的
                } else {
                    // 用户正常输入
                    onKeyboardNormalInput(primaryCode);
                }

                // 添加按键震动
                doVibrate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onText(CharSequence text) {
        }

        @Override
        public void swipeLeft() {
        }

        @Override
        public void swipeRight() {
        }

        @Override
        public void swipeDown() {
        }

        @Override
        public void swipeUp() {
        }
    };

    private void onKeyboardDeletePressed() {
        // 回退键,删除字符
        Editable editable = mCurrentEditText.getText();
        int start = mCurrentEditText.getSelectionStart();
        int end = mCurrentEditText.getSelectionEnd();
        if (editable != null && editable.length() > 0) {
            if (start == end) { //光标开始和结束位置相同, 即没有选中内容
                editable.delete(start - 1, start);
            } else { //光标开始和结束位置不同, 即选中EditText中的内容
                editable.delete(start, end);
            }
        }
    }

    private void onKeyboardLetterCaseChangePressed() {
        // 大小写切换, 只有字母键盘才有大小写切换功能
        changeKeyboardLetterCase();
        // 重新setKeyboard, 进而系统重新加载, 键盘内容才会变化(切换大小写)
        // keyboardType = 1;
        if (isCurrentEditTextKeyboardRandom()) {
            // 随机字母键盘
            sameEditSwitchKeyboard(11, keyboardLetterRandom);
        } else {
            // 正常字母键盘
            sameEditSwitchKeyboard(1, keyboardLetter);
        }
    }

    private void onKeyboardModeChangePressed() {
        // 数字与字母键盘互换
        if (isCurrentEditTextKeyboardRandom()) {
            if (keyboardType == 33) { //当前为能切换的数字键盘
                sameEditSwitchKeyboard(11, keyboardLetterRandom);
            } else {        //当前不是数字键盘, 能切换的数字键盘都是 3
                sameEditSwitchKeyboard(33, keyboardNumberRandom);
            }
        } else {
            if (keyboardType == 3) { //当前为能切换的数字键盘
                sameEditSwitchKeyboard(1, keyboardLetter);
            } else {        //当前不是数字键盘, 能切换的数字键盘都是 3
                sameEditSwitchKeyboard(3, keyboardNumber);
            }
        }
    }

    private void onKeyboardSybLetSwitchPressed() {
        // 字母与符号切换
        if (keyboardType == 2) { //当前是符号键盘
            if (isCurrentEditTextKeyboardRandom()) {
                sameEditSwitchKeyboard(11, keyboardLetterRandom);
            } else {
                sameEditSwitchKeyboard(1, keyboardLetter);
            }
        } else {        //当前不是符号键盘, 那么切换到符号键盘
            sameEditSwitchKeyboard(2, keyboardSymbol);
        }
    }

    private void onKeyboardNormalInput(int primaryCode) {
        // 用户正常输入
        Editable editable = mCurrentEditText.getText();
        int start = mCurrentEditText.getSelectionStart();
        int end = mCurrentEditText.getSelectionEnd();
        // editable.insert(start, Character.toString((char) primaryCode));
        editable.replace(start, end, Character.toString((char) primaryCode));

        Object tag = mCurrentEditText.getTag();
        int type = 1;
        if (tag != null) {
            Integer integer = mEditLastKeyboardTypeMap.get(tag.toString());
            type = integer == null ? 1 : integer;
        }

        if ((type == 1 || type == 11) && !isCapLock && isCapes) {
            // 这是大写未锁定, 大写时输入了一个字母, 键盘自动切换为小写, 这里如果是随机字母键盘时, 不应该再刷新键盘, 所以
            isJustChangeLetterCase = true;
            isCapes = false;
            toLowerCase();
            keyboardView.setCap(isCapes);
            keyboardView.setCapLock(isCapLock);
            // 这里其实不用切换键盘, 实际上也是没有切换的, 只是大小写切换, 需要更新一下而已
            // 和 onKeyboardLetterCaseChangePressed 其实是一个效果
            sameEditSwitchKeyboard(type, isCurrentEditTextKeyboardRandom() ? keyboardLetterRandom : keyboardLetter);
        }
    }

    private void doVibrate() {
        // 添加按键震动
        if (keyboardView != null && keyboardView.isVibrateEnable() && isVibrateEditSetContainThisEdit(mCurrentEditText)) {
            if (mVibrator == null) {
                mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (mVibrator != null) {
                mVibrator.vibrate(20);
            }
        }
    }

    private void refreshDigitKeyboard(Keyboard keyboard) {
        if (keyboard != null) {
            SparseArray<Keyboard.Key> randomKeys;
            if (keyboard == keyboardIdCardRandom) {
                // 如果是 IdCard 键盘
                randomKeys = randomIdCardDigitKeys;
            } else if (keyboard == keyboardNumberOnlyRandom) {
                // 纯数字键盘
                randomKeys = randomDigitNumOnlyKeys;
            } else {
                // 否则认为是 数字 键盘 带部分符号的
                randomKeys = randomDigitKeys;
            }
            HashSet<Integer> set = new HashSet<>();
            while (set.size() < 10) {
                int num = (int) (Math.random() * 10);
                if (set.add(num)) {
                    // set.size() - 1 表示目前是第几个数字按键
                    Keyboard.Key key = randomKeys.get(set.size() - 1 + 48);
                    key.label = num + "";
                    key.codes[0] = 48 + num;
                }
            }
        } else {
            Log.w(TAG, "Refresh Digit ERROR! Keyboard is null");
        }
    }

    private void refreshRandomLetterKeyboard(Keyboard keyboard) {
        if (isJustChangeLetterCase && !changeLetCaseRefreshRandom) {
            // 如果是因为切换大小写运行到这里, 且设置了切换大小写时禁止刷新随机键盘位置, 那么就直接返回, 不再随机一次
            //     因为当用户在字母键盘中需要切换大小写时, 如果允许往下执行, 会出现点击 "SHIFT" 图标后字母的位置发生变化,
            //     极易导致客户输入错误, 造成不良体验, 不利于客户输入, 所以在这里进行拦截, 保持切换前后字母位置一致
            // 支持开启切换大小写时刷新随机键盘位置, 可在外部设置
            isJustChangeLetterCase = false;
            Log.i(TAG, "Just Change Letter Case! DO NOT REFRESH LETTER POSITION!!!");
            return;
        }
        if (keyboard == keyboardLetterRandom) {
            SparseArray<Keyboard.Key> randomKeys = randomLetterKeys;
            HashSet<Integer> set = new HashSet<>();
            int aCode = 97;
            while (set.size() < 26) {
                int num = (int) (Math.random() * 26);
                if (set.add(num)) { // 判断 set.add 是否成功以防随机数重复
                    // set.size() - 1 + aCode 表示按顺序将 a-z 依次替换为 letterBindArray 中预存的第 num 个字母 (num是本次随机到的数字)
                    Keyboard.Key key = randomKeys.get(set.size() - 1 + aCode);
                    String label = letterBindArray.get(num);
                    if (label == null) {
                        label = "null";
                    }
                    if (TextUtils.isEmpty(label)) {
                        label = "null";
                    }

                    String s = letter2CodeMap.get(label);
                    int code = 32;  // 空格
                    if (s != null) {
                        code = Integer.parseInt(s);
                    }
                    key.label = (isCapLock || isCapes) ? label.toUpperCase() : label;
                    key.codes[0] = (isCapLock || isCapes) ? (code - 32) : code;
                }
            }
        } else {
            Log.e(TAG, "Refresh keyboard ERROR! It is not random letter keyboard!!!");
        }
    }

    /**
     * 输入类型分类
     * <li>1.  字母键盘</li>
     * <li>11. 字母键盘--随机</li>
     * <li>2.  符号键盘</li>
     * <li>3.  数字键盘</li>
     * <li>33. 数字键盘--随机</li>
     * <li>4.  纯数字键盘</li>
     * <li>44. 纯数字键盘--随机</li>
     * <li>5.  中国身份证键盘</li>
     * <li>55. 中国身份证键盘--随机</li>
     * <br>
     * <p>同一个 EditText 内因点击按键切换键盘类型</p>
     * <p>只有同一个 EditText 内因点击按键切换键盘时才会调用, 切换键盘需要更新当前键盘类型</p>
     *
     * @param type     键盘类型, 切换键盘需要更新当前键盘类型
     * @param keyboard 键盘
     */
    private void sameEditSwitchKeyboard(int type, Keyboard keyboard) {
        doSwitchKeyboard(type, keyboard);
        // hideSystemKeyBoard(mCurrentEditText);
    }

    private void doSwitchKeyboard(int type, Keyboard keyboard) {
        beforeSwitchKeyboardRefreshRandom(type, keyboard);
        keyboardView.setKeyboard(keyboard, type, mCusKeyBgMap.get(mCurrentEditText.getTag().toString()));
        keyboardType = type;
        mEditLastKeyboardTypeMap.put(mCurrentEditText.getTag().toString(), keyboardType);
    }

    private void beforeSwitchKeyboardRefreshRandom(int type, Keyboard keyboard) {
        switch (type) {
            case 11:
                // 字母键盘--随机
                if (isThisEditRandomKeyboardSwitchSybLetNumRefreshLetter(mCurrentEditText)) {
                    refreshRandomLetterKeyboard(keyboard);
                }
                break;
            case 33:
                // 数字键盘--随机
                if (isThisEditRandomKeyboardSwitchSybLetNumRefreshNum(mCurrentEditText)) {
                    refreshDigitKeyboard(keyboard);
                }
                break;
            case 44:
            case 55:
                // 这里感觉其实不会进, 因为目前只支持 字母、数字、符号 键盘相互切换, 纯数字键盘和身份证键盘是不支持切换的
                // 但是这里保留, 暂不删除

                // 纯数字键盘--随机, 中国身份证键盘--随机
                // 上述两个键盘在当前版本中, 不存在与其他键盘相互切换的情况, 所以运行到这里就必然是用户点击了这个 EditText
                // 那么, 就需要刷新数字所在位置
                refreshDigitKeyboard(keyboard);
                break;
        }
    }

    private void showKeyboard(boolean needAnimateOpt) {
        doShowKeyboard(prepareKeyboardAndConsBeforeShow());
        if (needAnimateOpt) {
            keyContainer.setVisibility(View.VISIBLE);
            keyContainer.clearAnimation();
            keyContainer.startAnimation(showAnimation);
        }
    }

    private void doShowKeyboard(Keyboard keyboard) {
        beforeShowKeyboardRefreshRandom(keyboard);
        keyboardView.setKeyboard(keyboard, keyboardType, mCusKeyBgMap.get(mCurrentEditText.getTag().toString()));
        mEditLastKeyboardTypeMap.put(mCurrentEditText.getTag().toString(), keyboardType);
    }

    /**
     * <p>手动点击、或者焦点变化才会触发, 相当于再一次显示, 刷新随机键盘位置</p>
     * <p>不同 EditText 间切换键盘时, 触发此功能</p>
     * <p>SafeKeyboard 未显示时, 触发此功能</p>
     *
     * @param keyboard 键盘
     */
    private void beforeShowKeyboardRefreshRandom(Keyboard keyboard) {
        switch (keyboardType) {
            case 11:
                refreshRandomLetterKeyboard(keyboard);
                break;
            case 33:
            case 44:
            case 55:
                // 数字键盘--随机, 纯数字键盘--随机, 中国身份证键盘--随机
                // 上述三个键盘在当前版本中, 不存在与其他键盘相互切换的情况, 所以运行到这里就必然是用户点击了这个 EditText
                // 那么, 就需要刷新数字所在位置
                refreshDigitKeyboard(keyboard);
                break;
        }
    }

    private void changeKeyboardLetterCase() {
        // 默认小写键盘, 点一下改为大写, 不点击其他按键再点击切换大小写则触发锁定大写功能
        if (!isCapes) {
            // 为小写时, 改为大写.
            toUpperCase();
        } else if (isCapLock) {
            // 当前是大写, 且已经锁定了大写, 那么在点击一次切换大小写, 就需要改成小写键盘.
            toLowerCase();
        }
        if (isCapLock) {
            isCapLock = isCapes = false;
        } else if (isCapes) {
            isCapLock = true;
        } else {
            isCapes = true;
            // isCapLock = false; // 本身就是 false, 不要重复赋值
        }
        keyboardView.setCap(isCapes);
        keyboardView.setCapLock(isCapLock);
        isJustChangeLetterCase = true;
    }

    private void toLowerCase() {
        boolean isKeyboardRandom = isCurrentEditTextKeyboardRandom();
        Keyboard keyboard = isKeyboardRandom ? keyboardLetterRandom : keyboardLetter;
        List<Keyboard.Key> keyList = keyboard.getKeys();
        for (Keyboard.Key key : keyList) {
            if (key.label != null && isUpCaseLetter(key.label.toString())) {
                key.label = key.label.toString().toLowerCase();
                key.codes[0] += 32;
            }
        }
    }

    private void toUpperCase() {
        boolean isKeyboardRandom = isCurrentEditTextKeyboardRandom();
        Keyboard keyboard = isKeyboardRandom ? keyboardLetterRandom : keyboardLetter;
        List<Keyboard.Key> keyList = keyboard.getKeys();
        for (Keyboard.Key key : keyList) {
            if (key.label != null && isLowCaseLetter(key.label.toString())) {
                key.label = key.label.toString().toUpperCase();
                key.codes[0] -= 32;
            }
        }
    }

    public void hideKeyboard() {
        keyContainer.clearAnimation();
        keyContainer.startAnimation(hideAnimation);
    }

    private void doShowEnd() {
        isShowStart = false;
        // 在迅速点击不同输入框时, 造成自定义软键盘和系统软件盘不停的切换, 偶尔会出现停在使用系统键盘的输入框时, 没有隐藏
        // 自定义软键盘的情况, 为了杜绝这个现象, 加上下面这段代码
        if (!mCurrentEditText.isFocused()) {
            safeHandler.removeCallbacks(hideRun);
            safeHandler.removeCallbacks(showRun);
            safeHandler.postDelayed(hideRun, keyboardConfig.hideDelay);
        }

        // 这个只能在 keyContainer 显示后才能调用, 只有这个时候才能获取到 keyContainer 的宽、高值
        doScrollLayout();
    }

    private void doHideEnd() {
        isHideStart = false;

        doScrollLayoutBack(true, null);

        keyContainer.clearAnimation();
        if (keyContainer.getVisibility() != View.GONE) {
            keyContainer.setVisibility(View.GONE);
        }
    }

    /**
     * 回落
     *
     * @param isHide 回落的同时, SafeKeyboard 是否隐藏
     */
    private boolean doScrollLayoutBack(final boolean isHide, EditText mEditText) {
        int thisScrollY = 0;
        if (!isHide && mEditText != null) {
            // 这种情况说明是点击了一个 EditText, 则需要判断是否需要移动 mScrollLayout 来适应 SafeKeyboard 的显示
            int[] mEditPos = new int[2];
            mEditText.getLocationOnScreen(mEditPos);
            // Log.e("SafeKeyboard_Scroll", "0: " + mEditPos[0] + ", 1: " + mEditPos[1]);

            int keyboardHeight = keyContainer.getHeight();
            int keyStartY = mScreenHeight - keyboardHeight;
            getOriginalScrollLayoutPos();

            if (mEditText.getHeight() + 10 > keyStartY - originalScrollPosInScr[1]) {
                // mEditText 的高度 大于 SafeKeyboard 上边界到 mScrollLayout 上边界的距离, 即 mEditText 无法完全显示
                // TODO... 添加一个长文本输入功能

                return false;
            } else {
                // 可以正常显示
                if (mEditPos[1] < originalScrollPosInScr[1]) {
                    // 说明当前的 mEditText 的 top 位置已经被其他布局遮挡, 需要布局往下滑动一点, 使 mEditText 可以完全显示
                    thisScrollY = originalScrollPosInScr[1] - mEditPos[1] + 10; // 正值
                } else if (mEditPos[1] + mEditText.getHeight() > keyStartY) {
                    // 说明当前的 mEditText 的 bottom 位置已经被其他布局遮挡, 需要布局往上滑动一点, 使 mEditText 可以完全显示
                    thisScrollY = keyStartY - mEditPos[1] - mEditText.getHeight(); //负值
                } else {
                    // 各项均正常, 不需要重新滑动
                    Log.i(TAG, "Need not to scroll");
                    return false;
                }
            }
        }

        toBackSize += thisScrollY;
        if (isHide) {
            mScrollLayout.animate().setDuration(keyboardConfig.showDuration).translationYBy(-toBackSize).start();
            toBackSize = 0;
        } else {
            mScrollLayout.animate().setDuration(keyboardConfig.showDuration).translationYBy(thisScrollY).start();
        }

        return true;
    }

    /**
     * 顶起
     */
    private void doScrollLayout() {
        // 计算 SafeKeyboard 显示后是否会遮挡住 EditText
        editNeedScroll(mCurrentEditText);
    }

    /**
     * @param mEditText 目标 EditText
     */
    private void editNeedScroll(EditText mEditText) {
        int keyboardHeight = keyContainer.getHeight();      // 获取键盘布局的高度
        int keyStartY = mScreenHeight - keyboardHeight;
        int[] position = new int[2];
        mEditText.getLocationOnScreen(position);
        int mEditTextBottomY = position[1] + mEditText.getHeight();
        if (mEditTextBottomY > keyStartY) {
            // 说明这个 EditText 的底部在 键盘 View 顶部以下, 即 EditText 被键盘遮挡了
            final float to = keyStartY - mEditTextBottomY - 10; // 为负值, 需要往上移动的距离, 往上为负值, 往下为正值
            if (position[1] + to < originalScrollPosInScr[1]) {
                // 说明, scrollLayout 被往上顶起之后, EditText 所在位置可能会被 scrollLayout 上面的其他 View 遮挡或者重合了导致显示不准确,
                // 那么顶起操作在这里就显示不合适了, 所以这里最好是添加一个长文本显示功能
                // 说明往上顶起之后 mEditText 会被遮挡, 即 mEditText 的 top 距离顶部的距离 小于 要移动的距离
                // 这里就不需要顶起了, 需要显示一个长文本显示页面
                // TODO... 添加一个长文本显示功能, 不过这里的长文本显示似乎没有什么意义
                return;
            }
            toBackSize = to;
            mScrollLayout.animate().translationYBy(toBackSize).setDuration(keyboardConfig.showDuration).start();
        }
    }

    /**
     * 在显示 SafeKeyboard 之前, 先计算好相关的变量信息, 以及对应的 Keyboard
     *
     * @return Keyboard 键盘
     */
    private @NonNull Keyboard prepareKeyboardAndConsBeforeShow() {
        boolean isKeyboardRandom = isCurrentEditTextKeyboardRandom();
        // 计算相关信息
        // keyboardType, keyboard
        Keyboard curKeyboard; // 默认字母键盘
        int inputType = mCurrentEditText.getInputType(); // 这里 inputType 与 InputType 里面的不是一一对应的, 无法全部判断
        if (inputType == InputType.TYPE_CLASS_NUMBER || inputType == 18) {
            // 18 代表数字密码
            // 纯数字键盘
            keyboardType = isKeyboardRandom ? 44 : 4;
            curKeyboard = isKeyboardRandom ? keyboardNumberOnlyRandom : keyboardNumberOnly;
        } else if (inputType == 4098 || inputType == 8194 || inputType == 3
                // numberSigned         numberDecimal        phone 就是 InputType.TYPE_CLASS_PHONE
                || inputType == 20 || inputType == 36 || inputType == 4
                //  date               time               datetime 就是 TYPE_CLASS_DATETIME
                || inputType == InputType.TYPE_NUMBER_FLAG_SIGNED || inputType == InputType.TYPE_NUMBER_FLAG_DECIMAL) {

            // 数字键盘, 就是带符号的数字家农安
            // 这里就是用户可以设置的和数字相关的非纯数字的输入类型, 默认赋值为带符号的数字键盘
            keyboardType = isKeyboardRandom ? 33 : 3;
            curKeyboard = isKeyboardRandom ? keyboardNumberRandom : keyboardNumber;
            // 输入类型的就到这里, 其他的默认为字母键盘(被设置了其他键盘的除外)
        } else if (isIdCardEditSetContainThisEdit(mCurrentEditText)) {
            // 身份证键盘
            keyboardType = isKeyboardRandom ? 55 : 5;
            curKeyboard = isKeyboardRandom ? keyboardIdCardRandom : keyboardIdCard;
        } else {
            // 字母键盘
            // 其他全部默认字母键盘, 可在字母、数字、符号三者之间切换
            // 初始化时为字母键盘, 切换为符号时由其他逻辑处理

            Object tag = mCurrentEditText.getTag();
            // 这里一般来说不可能是 null, 因为每个 EditText 在 putEditText 时全都已经设置了
            if (tag == null) {
                keyboardType = isKeyboardRandom ? 11 : 1;
                curKeyboard = isKeyboardRandom ? keyboardLetterRandom : keyboardLetter;
            } else {
                // keyboardView.isRememberLastType(): 是否开启记录 EditText 上次输入结束后的键盘类型
                // 开启后, 当再次点击该 EditText 时, 仍然显示上次的键盘类型
                // 例如: 默认点击 EditTextA 并输入字母后(或不输入), 切换为字符键盘, 然后直接点击页面中其他 EditTextB,
                //      而后当再次点击 EditTextA 时, 会直接显示切换时最后输入的字符键盘。
                //      本功能默认关闭, 需设置打开。
                Integer integer = keyboardView.isRememberLastType() ? mEditLastKeyboardTypeMap.get(tag.toString()) : null;
                keyboardType = integer == null ? (isKeyboardRandom ? 11 : 1) : integer;
                curKeyboard = getKeyboardByCurKeyboardType();
            }
        }
        return curKeyboard;
    }

    /**
     * 调用这里之前必须确保已经设置了 mCurrentEditText
     *
     * <p>输入类型分类</p>
     * <li>1.  字母键盘</li>
     * <li>11. 字母键盘--随机</li>
     * <li>2.  符号键盘</li>
     * <li>3.  数字键盘</li>
     * <li>33. 数字键盘--随机</li>
     * <li>4.  纯数字键盘</li>
     * <li>44. 纯数字键盘--随机</li>
     * <li>5.  中国身份证键盘</li>
     * <li>55. 中国身份证键盘--随机</li>
     * <br>
     * <p>同一个 EditText 内因点击按键切换键盘类型</p>
     *
     * @return 返回 Keyboard 对象
     */
    private @NonNull Keyboard getKeyboardByCurKeyboardType() {
        // 默认字母键盘
        Keyboard curKeyboard;
        switch (keyboardType) {
            case 1:
                curKeyboard = keyboardLetter;
                break;
            case 11:
                curKeyboard = keyboardLetterRandom;
                break;
            case 2:
                curKeyboard = keyboardSymbol;
                break;
            case 3:
                curKeyboard = keyboardNumber;
                break;
            case 33:
                curKeyboard = keyboardNumberRandom;
                break;
            case 4:
                curKeyboard = keyboardNumberOnly;
                break;
            case 44:
                curKeyboard = keyboardNumberOnlyRandom;
                break;
            case 5:
                curKeyboard = keyboardIdCard;
                break;
            case 55:
                curKeyboard = keyboardIdCardRandom;
                break;
            default:
                curKeyboard = isCurrentEditTextKeyboardRandom() ? keyboardLetterRandom : keyboardLetter;
                Log.e(TAG, "ERROR keyboard type: " + keyboardType + "!!! transfer to default letter keyboard.");
                break;
        }
        // 其余的情况不考虑, 均视为普通字母键盘
        return curKeyboard;
    }

    private boolean isLowCaseLetter(String str) {
        String letters = "abcdefghijklmnopqrstuvwxyz";
        return letters.contains(str);
    }

    private boolean isUpCaseLetter(String str) {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        return letters.contains(str);
    }

    private boolean isEditSetContainThisEdit(EditText mEditText) {
        Object tagObj = mEditText.getTag();
        return tagObj != null && mEditTagSet.contains(tagObj.toString());
    }

    private boolean isIdCardEditSetContainThisEdit(EditText mEditText) {
        Object tagObj = mEditText.getTag();
        return tagObj != null && mIdCardEditTagSet.contains(tagObj.toString());
    }

    private boolean isCurrentEditTextKeyboardRandom() {
        return isThisEditKeyboardRandom(mCurrentEditText);
    }

    private boolean isThisEditKeyboardRandom(EditText mEditText) {
        return mRandomEditTagSet != null && !mRandomEditTagSet.isEmpty() && mRandomEditTagSet.contains(mEditText.getTag().toString());
    }

    private boolean isThisEditRandomKeyboardSwitchSybLetNumRefreshLetter(EditText mEditText) {
        return mRandomSybLetKbdSwitchRefreshEditTagSet != null && !mRandomSybLetKbdSwitchRefreshEditTagSet.isEmpty() && mRandomSybLetKbdSwitchRefreshEditTagSet.contains(mEditText.getTag().toString());
    }

    private boolean isThisEditRandomKeyboardSwitchSybLetNumRefreshNum(EditText mEditText) {
        return mRndNumKbdSwitchAlwaysRefreshEditTagSet != null && !mRndNumKbdSwitchAlwaysRefreshEditTagSet.isEmpty() && mRndNumKbdSwitchAlwaysRefreshEditTagSet.contains(mEditText.getTag().toString());
    }

    private boolean isVibrateEditSetContainThisEdit(EditText mEditText) {
        Object tagObj = mEditText.getTag();
        return tagObj != null && mVibrateEditTagSet.contains(tagObj.toString());
    }

    private void setCurrentEditText(EditText mEditText) {
        mCurrentEditText = mEditText;
    }

    public void setVibrateEnable(boolean vibrateEnable) {
        isVibrateEnable = vibrateEnable;
        // 加上下面这个可以规避 putVibrateEdit() 和 setVibrateEnable() 先后调用导致可能无法震动的问题
        if (mVibrateEditTagSet != null && !mVibrateEditTagSet.isEmpty()) {
            keyboardView.enableVibrate();
        }
    }

    public void setEnableCancelInput(boolean enableCancelInput) {
        this.enableCancelInput = enableCancelInput;
    }

    public boolean isShow() {
        return isKeyboardShown();
    }

    //隐藏系统键盘关键代码
    private void hideSystemKeyBoard(EditText edit) {
        // 这里不应该给 mCurrentEditText 赋值, 先注释掉
        // this.mCurrentEditText = edit;
        InputMethodManager imm = (InputMethodManager) this.mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        boolean isOpen = imm.isActive();
        if (isOpen) {
            imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
        }

        int currentVersion = Build.VERSION.SDK_INT;
        String methodName = null;
        if (currentVersion >= 16) {
            methodName = "setShowSoftInputOnFocus";
            // edit.setShowSoftInputOnFocus(false);
        } else if (currentVersion >= 14) {
            methodName = "setSoftInputShownOnFocus";
        }

        if (methodName == null) {
            edit.setInputType(0);
        } else {
            try {
                Method setShowSoftInputOnFocus = EditText.class.getMethod(methodName, Boolean.TYPE);
                setShowSoftInputOnFocus.setAccessible(true);
                setShowSoftInputOnFocus.invoke(edit, Boolean.FALSE);
            } catch (NoSuchMethodException e) {
                edit.setInputType(0);
                e.printStackTrace();
            } catch (IllegalAccessException | InvocationTargetException |
                     IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isKeyboardShown() {
        return keyContainer.getVisibility() == View.VISIBLE;
    }

    public boolean stillNeedOptManually(boolean preferShow) {
        boolean flag;
        if (preferShow) {
            // 想要显示
            flag = isHideStart || (!isKeyboardShown() && !isShowStart);
        } else {
            // 想要隐藏
            flag = isShowStart || (isKeyboardShown() && !isHideStart);
        }
        return flag;
    }

    private boolean isValidTouch() {
        long thisTouchTime = SystemClock.elapsedRealtime();
        if (thisTouchTime - lastTouchTime > 500) {
            lastTouchTime = thisTouchTime;
            return true;
        }
        lastTouchTime = thisTouchTime;
        return false;
    }

    public void release() {
        mContext = null;
        isCapes = false;
        isVibrateEnable = false;
        toBackSize = 0;
        lastTouchTime = 0L;
        onEditTextTouchListener = null;
        if (treeObserver != null && onGlobalFocusChangeListener != null && treeObserver.isAlive()) {
            treeObserver.removeOnGlobalFocusChangeListener(onGlobalFocusChangeListener);
        }
        treeObserver = null;
        onGlobalFocusChangeListener = null;
        if (mEditLastKeyboardTypeMap != null) {
            mEditLastKeyboardTypeMap.clear();
            mEditLastKeyboardTypeMap = null;
        }
        if (mEditTagSet != null) {
            mEditTagSet.clear();
            mEditTagSet = null;
        }
        if (mIdCardEditTagSet != null) {
            mIdCardEditTagSet.clear();
            mIdCardEditTagSet = null;
        }
        if (mRandomEditTagSet != null) {
            mRandomEditTagSet.clear();
            mRandomEditTagSet = null;
        }
        if (mRandomSybLetKbdSwitchRefreshEditTagSet != null) {
            mRandomSybLetKbdSwitchRefreshEditTagSet.clear();
            mRandomSybLetKbdSwitchRefreshEditTagSet = null;
        }
        if (mRndNumKbdSwitchAlwaysRefreshEditTagSet != null) {
            mRndNumKbdSwitchAlwaysRefreshEditTagSet.clear();
            mRndNumKbdSwitchAlwaysRefreshEditTagSet = null;
        }
        if (mVibrateEditTagSet != null) {
            mVibrateEditTagSet.clear();
            mVibrateEditTagSet = null;
        }
        mVibrator = null;

        mScreenWidth = 0;
        mScreenHeight = 0;
        originalScrollPosInScr = null;
        originalScrollPosInPar = null;
    }
}
