// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import helium314.keyboard.keyboard.internal.KeyboardIconsSet;
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode;
import helium314.keyboard.latin.R;
import helium314.keyboard.latin.common.ColorType;
import helium314.keyboard.latin.common.Colors;
import helium314.keyboard.latin.common.Constants;
import helium314.keyboard.latin.settings.Settings;

public class TextEditView extends LinearLayout {

    public interface TextEditListener {
        void onCursorMove(int keyCode, boolean isSelecting);
        void onCodeInput(int keyCode);
        void onClose();
    }

    private TextEditListener mListener;
    private boolean mSelectionMode = false;

    // Buttons
    private TextView mBtnUndo;
    private TextView mBtnSelect;
    private TextView mBtnCut;
    private TextView mBtnCopy;
    private TextView mBtnPaste;
    private ImageView mBtnClose;

    private ImageView mBtnHome;
    private ImageView mBtnWordLeft;
    private ImageView mBtnArrowUp;
    private ImageView mBtnWordRight;
    private ImageView mBtnEnd;

    private ImageView mBtnBackspace;
    private ImageView mBtnArrowLeft;
    private ImageView mBtnArrowDown;
    private ImageView mBtnArrowRight;
    private ImageView mBtnSpace;

    public TextEditView(Context context) {
        super(context);
        init(context);
    }

    public TextEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TextEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setClickable(true);
        setFocusable(true);
        setFitsSystemWindows(true);

        LayoutInflater.from(context).inflate(R.layout.text_edit_view, this, true);

        mBtnUndo = findViewById(R.id.btn_undo);
        mBtnSelect = findViewById(R.id.btn_select);
        mBtnCut = findViewById(R.id.btn_cut);
        mBtnCopy = findViewById(R.id.btn_copy);
        mBtnPaste = findViewById(R.id.btn_paste);
        mBtnClose = findViewById(R.id.btn_close);

        mBtnHome = findViewById(R.id.btn_home);
        mBtnWordLeft = findViewById(R.id.btn_word_left);
        mBtnArrowUp = findViewById(R.id.btn_arrow_up);
        mBtnWordRight = findViewById(R.id.btn_word_right);
        mBtnEnd = findViewById(R.id.btn_end);

        mBtnBackspace = findViewById(R.id.btn_backspace);
        mBtnArrowLeft = findViewById(R.id.btn_arrow_left);
        mBtnArrowDown = findViewById(R.id.btn_arrow_down);
        mBtnArrowRight = findViewById(R.id.btn_arrow_right);
        mBtnSpace = findViewById(R.id.btn_space);

        setupClickListeners();
    }

    // ponytail: simplified touch feedback and repeatability
    private void setTouchHandler(View view, boolean repeatable, Runnable action, Runnable longPressAction) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            private boolean isInside = false;
            private boolean isLongPressed = false;

            private final Runnable repeatableRunnable = new Runnable() {
                @Override
                public void run() {
                    action.run();
                    handler.postDelayed(this, 80);
                }
            };

            private final Runnable longPressRunnable = new Runnable() {
                @Override
                public void run() {
                    isLongPressed = true;
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    longPressAction.run();
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isInside = true;
                        isLongPressed = false;
                        v.setScaleX(0.92f);
                        v.setScaleY(0.92f);
                        v.setAlpha(0.7f);
                        if (repeatable) {
                            action.run();
                            handler.postDelayed(repeatableRunnable, 400);
                        } else if (longPressAction != null) {
                            handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start();
                        if (repeatable) {
                            handler.removeCallbacks(repeatableRunnable);
                        } else {
                            handler.removeCallbacks(longPressRunnable);
                            if (isInside && !isLongPressed) {
                                action.run();
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start();
                        if (repeatable) {
                            handler.removeCallbacks(repeatableRunnable);
                        } else {
                            handler.removeCallbacks(longPressRunnable);
                        }
                        isInside = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float x = event.getX();
                        float y = event.getY();
                        boolean nowInside = x >= 0 && x <= v.getWidth() && y >= 0 && y <= v.getHeight();
                        if (nowInside != isInside) {
                            isInside = nowInside;
                            if (!isInside) {
                                v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start();
                                if (repeatable) {
                                    handler.removeCallbacks(repeatableRunnable);
                                } else {
                                    handler.removeCallbacks(longPressRunnable);
                                }
                            } else {
                                v.setScaleX(0.92f);
                                v.setScaleY(0.92f);
                                v.setAlpha(0.7f);
                                if (longPressAction != null && !isLongPressed) {
                                    handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout() - event.getEventTime() + event.getDownTime());
                                }
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void setupClickListeners() {
        setTouchHandler(mBtnUndo, false, () -> {
            if (mListener != null) mListener.onCodeInput(KeyCode.UNDO);
        }, null);

        setTouchHandler(mBtnSelect, false, () -> {
            mSelectionMode = !mSelectionMode;
            KeyboardActionListenerImpl.sPersistentSelectionModeActive = mSelectionMode;
            applyColors(Settings.getValues().mColors);
        }, () -> {
            if (mListener != null) mListener.onCodeInput(KeyCode.CLIPBOARD_SELECT_ALL);
        });

        setTouchHandler(mBtnCut, false, () -> {
            if (mListener != null) mListener.onCodeInput(KeyCode.CLIPBOARD_CUT);
            mSelectionMode = false;
            applyColors(Settings.getValues().mColors);
        }, null);

        setTouchHandler(mBtnCopy, false, () -> {
            if (mListener != null) mListener.onCodeInput(KeyCode.CLIPBOARD_COPY);
            mSelectionMode = false;
            applyColors(Settings.getValues().mColors);
        }, null);

        setTouchHandler(mBtnPaste, false, () -> {
            if (mListener != null) mListener.onCodeInput(KeyCode.CLIPBOARD_PASTE);
        }, null);

        setTouchHandler(mBtnClose, false, () -> {
            if (mListener != null) mListener.onClose();
        }, null);

        setTouchHandler(mBtnHome, false, () -> {
            if (mListener != null) mListener.onCodeInput(KeyCode.MOVE_START_OF_PAGE);
        }, null);

        setTouchHandler(mBtnWordLeft, true, () -> {
            if (mListener != null) mListener.onCodeInput(KeyCode.WORD_LEFT);
        }, null);

        setTouchHandler(mBtnArrowUp, true, () -> {
            if (mListener != null) mListener.onCursorMove(KeyCode.ARROW_UP, mSelectionMode || KeyboardActionListenerImpl.sPersistentSelectionModeActive);
        }, null);

        setTouchHandler(mBtnWordRight, true, () -> {
            if (mListener != null) mListener.onCodeInput(KeyCode.WORD_RIGHT);
        }, null);

        setTouchHandler(mBtnEnd, false, () -> {
            if (mListener != null) mListener.onCodeInput(KeyCode.MOVE_END_OF_PAGE);
        }, null);

        setTouchHandler(mBtnBackspace, true, () -> {
            if (mListener != null) mListener.onCodeInput(KeyCode.DELETE);
        }, null);

        setTouchHandler(mBtnArrowLeft, true, () -> {
            if (mListener != null) mListener.onCursorMove(KeyCode.ARROW_LEFT, mSelectionMode || KeyboardActionListenerImpl.sPersistentSelectionModeActive);
        }, null);

        setTouchHandler(mBtnArrowDown, true, () -> {
            if (mListener != null) mListener.onCursorMove(KeyCode.ARROW_DOWN, mSelectionMode || KeyboardActionListenerImpl.sPersistentSelectionModeActive);
        }, null);

        setTouchHandler(mBtnArrowRight, true, () -> {
            if (mListener != null) mListener.onCursorMove(KeyCode.ARROW_RIGHT, mSelectionMode || KeyboardActionListenerImpl.sPersistentSelectionModeActive);
        }, null);

        setTouchHandler(mBtnSpace, false, () -> {
            if (mListener != null) mListener.onCodeInput(Constants.CODE_SPACE);
        }, null);
    }

    public void setTextEditListener(TextEditListener listener) {
        mListener = listener;
    }

    private int getCompositeColor(int srcColor, int dstColor) {
        int alpha = (srcColor >> 24) & 0xFF;
        if (alpha == 0xFF) return srcColor;
        if (alpha == 0x00) return dstColor;
        
        float fAlpha = alpha / 255f;
        int srcR = (srcColor >> 16) & 0xFF;
        int srcG = (srcColor >> 8) & 0xFF;
        int srcB = srcColor & 0xFF;
        
        int dstR = (dstColor >> 16) & 0xFF;
        int dstG = (dstColor >> 8) & 0xFF;
        int dstB = dstColor & 0xFF;
        
        int r = Math.round(srcR * fAlpha + dstR * (1 - fAlpha));
        int g = Math.round(srcG * fAlpha + dstG * (1 - fAlpha));
        int b = Math.round(srcB * fAlpha + dstB * (1 - fAlpha));
        
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private int getContrastingColor(int bgColor, Colors colors) {
        int compositeBg = getCompositeColor(bgColor, colors.get(ColorType.MAIN_BACKGROUND));
        double Lbg = androidx.core.graphics.ColorUtils.calculateLuminance(compositeBg);
        double Lwhite = 0.95;
        double Lblack = 0.015;
        double ratioWhite = Lbg > Lwhite ? (Lbg + 0.05) / (Lwhite + 0.05) : (Lwhite + 0.05) / (Lbg + 0.05);
        double ratioBlack = Lbg > Lblack ? (Lbg + 0.05) / (Lblack + 0.05) : (Lblack + 0.05) / (Lbg + 0.05);
        return ratioWhite > ratioBlack ? 0xFFFAFAFA : 0xFF222222;
    }

    public void applyColors(Colors colors) {
        colors.setBackground(this, ColorType.MAIN_BACKGROUND);

        int baseBg = colors.get(ColorType.FUNCTIONAL_KEY_BACKGROUND);
        int normalBg = colors.get(ColorType.KEY_BACKGROUND);
        int spaceBg = colors.get(ColorType.SPACE_BAR_BACKGROUND);
        int actionBg = colors.get(ColorType.ACTION_KEY_BACKGROUND);

        // Compute custom color variants for different types of functions
        // 1. Clipboard operations (Select, Cut, Copy, Paste): Blue-tinted functional background
        int clipboardBg = androidx.core.graphics.ColorUtils.blendARGB(baseBg, 0xFF2196F3, 0.15f);
        int selectBg = (mSelectionMode || KeyboardActionListenerImpl.sPersistentSelectionModeActive) ? actionBg : clipboardBg;
        int clipboardTextColor = getContrastingColor(clipboardBg, colors);
        int selectTextColor = getContrastingColor(selectBg, colors);

        // 2. Text manipulation (Undo, Backspace): Orange-tinted functional background
        int editBg = androidx.core.graphics.ColorUtils.blendARGB(baseBg, 0xFFFF9800, 0.15f);
        int editTextColor = getContrastingColor(editBg, colors);

        // 3. Navigation controls (Home, End, Word Left, Word Right): Green-tinted functional background
        int navBg = androidx.core.graphics.ColorUtils.blendARGB(baseBg, 0xFF4CAF50, 0.12f);
        int navTextColor = getContrastingColor(navBg, colors);

        // 4. System / Close button: Red-tinted functional background
        int closeBg = androidx.core.graphics.ColorUtils.blendARGB(baseBg, 0xFFF44336, 0.15f);
        int closeTextColor = getContrastingColor(closeBg, colors);

        // 5. Arrow Keys (Up, Down, Left, Right): Standard key background
        int arrowBg = normalBg;
        int arrowTextColor = getContrastingColor(arrowBg, colors);

        // 6. Space Bar: Space bar background
        int spaceBtnBg = spaceBg;
        int spaceTextColor = getContrastingColor(spaceBtnBg, colors);

        // Apply background and text/icon colors to Action Buttons
        setKeyStyle(mBtnUndo, editBg, editTextColor);
        setKeyStyle(mBtnSelect, selectBg, selectTextColor);
        setKeyStyle(mBtnCut, clipboardBg, clipboardTextColor);
        setKeyStyle(mBtnCopy, clipboardBg, clipboardTextColor);
        setKeyStyle(mBtnPaste, clipboardBg, clipboardTextColor);

        // Retrieve theme-aware icons
        KeyboardSwitcher switcher = KeyboardSwitcher.getInstance();
        KeyboardIconsSet iconsSet = (switcher != null && switcher.getKeyboard() != null) ? switcher.getKeyboard().mIconsSet : null;

        setIconKeyStyle(mBtnClose, iconsSet, "close_history", closeBg, closeTextColor);
        setIconKeyStyle(mBtnHome, iconsSet, "page_start", navBg, navTextColor);
        setIconKeyStyle(mBtnWordLeft, iconsSet, "word_left", navBg, navTextColor);
        setIconKeyStyle(mBtnArrowUp, iconsSet, "up", arrowBg, arrowTextColor);
        setIconKeyStyle(mBtnWordRight, iconsSet, "word_right", navBg, navTextColor);
        setIconKeyStyle(mBtnEnd, iconsSet, "page_end", navBg, navTextColor);
        setIconKeyStyle(mBtnBackspace, iconsSet, "delete_key", editBg, editTextColor);
        setIconKeyStyle(mBtnArrowLeft, iconsSet, "left", arrowBg, arrowTextColor);
        setIconKeyStyle(mBtnArrowDown, iconsSet, "down", arrowBg, arrowTextColor);
        setIconKeyStyle(mBtnArrowRight, iconsSet, "right", arrowBg, arrowTextColor);
        setIconKeyStyle(mBtnSpace, iconsSet, "space_key_for_number_layout", spaceBtnBg, spaceTextColor);
    }

    private void setKeyStyle(TextView textView, int bgColor, int textColor) {
        textView.setBackground(createKeyBackground(bgColor));
        textView.setTextColor(textColor);
    }

    private void setIconKeyStyle(ImageView imageView, KeyboardIconsSet iconsSet, String iconName, int bgColor, int iconColor) {
        imageView.setBackground(createKeyBackground(bgColor));
        if (iconsSet != null) {
            Drawable icon = iconsSet.getIconDrawable(iconName);
            if (icon != null) {
                Drawable mutated = icon.mutate();
                mutated.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                imageView.setImageDrawable(mutated);
            }
        }
    }

    private Drawable createKeyBackground(int color) {
        float density = getContext().getResources().getDisplayMetrics().density;
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(6f * density);
        gd.setColor(color);
        return gd;
    }
}
