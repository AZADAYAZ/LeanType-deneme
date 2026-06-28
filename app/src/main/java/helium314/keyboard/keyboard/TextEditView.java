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
                    handler.postDelayed(this, 50);
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

        setTouchHandler(mBtnWordLeft, false, () -> {
            if (mListener != null) mListener.onCodeInput(KeyCode.WORD_LEFT);
        }, null);

        setTouchHandler(mBtnArrowUp, true, () -> {
            if (mListener != null) mListener.onCursorMove(KeyCode.ARROW_UP, mSelectionMode);
        }, null);

        setTouchHandler(mBtnWordRight, false, () -> {
            if (mListener != null) mListener.onCodeInput(KeyCode.WORD_RIGHT);
        }, null);

        setTouchHandler(mBtnEnd, false, () -> {
            if (mListener != null) mListener.onCodeInput(KeyCode.MOVE_END_OF_PAGE);
        }, null);

        setTouchHandler(mBtnBackspace, true, () -> {
            if (mListener != null) mListener.onCodeInput(KeyCode.DELETE);
        }, null);

        setTouchHandler(mBtnArrowLeft, true, () -> {
            if (mListener != null) mListener.onCursorMove(KeyCode.ARROW_LEFT, mSelectionMode);
        }, null);

        setTouchHandler(mBtnArrowDown, true, () -> {
            if (mListener != null) mListener.onCursorMove(KeyCode.ARROW_DOWN, mSelectionMode);
        }, null);

        setTouchHandler(mBtnArrowRight, true, () -> {
            if (mListener != null) mListener.onCursorMove(KeyCode.ARROW_RIGHT, mSelectionMode);
        }, null);

        setTouchHandler(mBtnSpace, false, () -> {
            if (mListener != null) mListener.onCodeInput(Constants.CODE_SPACE);
        }, null);
    }

    public void setTextEditListener(TextEditListener listener) {
        mListener = listener;
    }

    public void applyColors(Colors colors) {
        colors.setBackground(this, ColorType.MAIN_BACKGROUND);

        int keyTextColor = colors.get(ColorType.KEY_TEXT);
        int functionalKeyTextColor = colors.get(ColorType.FUNCTIONAL_KEY_TEXT);
        int keyIconColor = colors.get(ColorType.KEY_ICON);

        // Apply background and text colors to Action Buttons
        setKeyStyle(mBtnUndo, colors, ColorType.FUNCTIONAL_KEY_BACKGROUND, keyTextColor);
        setKeyStyle(mBtnSelect, colors, mSelectionMode ? ColorType.ACTION_KEY_BACKGROUND : ColorType.FUNCTIONAL_KEY_BACKGROUND, mSelectionMode ? functionalKeyTextColor : keyTextColor);
        setKeyStyle(mBtnCut, colors, ColorType.FUNCTIONAL_KEY_BACKGROUND, keyTextColor);
        setKeyStyle(mBtnCopy, colors, ColorType.FUNCTIONAL_KEY_BACKGROUND, keyTextColor);
        setKeyStyle(mBtnPaste, colors, ColorType.FUNCTIONAL_KEY_BACKGROUND, keyTextColor);

        // Retrieve theme-aware icons
        KeyboardSwitcher switcher = KeyboardSwitcher.getInstance();
        KeyboardIconsSet iconsSet = (switcher != null && switcher.getKeyboard() != null) ? switcher.getKeyboard().mIconsSet : null;

        setIconKeyStyle(mBtnClose, iconsSet, "close_history", colors, ColorType.FUNCTIONAL_KEY_BACKGROUND, keyIconColor);
        setIconKeyStyle(mBtnHome, iconsSet, "page_start", colors, ColorType.FUNCTIONAL_KEY_BACKGROUND, keyIconColor);
        setIconKeyStyle(mBtnWordLeft, iconsSet, "word_left", colors, ColorType.FUNCTIONAL_KEY_BACKGROUND, keyIconColor);
        setIconKeyStyle(mBtnArrowUp, iconsSet, "up", colors, ColorType.KEY_BACKGROUND, keyIconColor);
        setIconKeyStyle(mBtnWordRight, iconsSet, "word_right", colors, ColorType.FUNCTIONAL_KEY_BACKGROUND, keyIconColor);
        setIconKeyStyle(mBtnEnd, iconsSet, "page_end", colors, ColorType.FUNCTIONAL_KEY_BACKGROUND, keyIconColor);
        setIconKeyStyle(mBtnBackspace, iconsSet, "delete_key", colors, ColorType.FUNCTIONAL_KEY_BACKGROUND, keyIconColor);
        setIconKeyStyle(mBtnArrowLeft, iconsSet, "left", colors, ColorType.KEY_BACKGROUND, keyIconColor);
        setIconKeyStyle(mBtnArrowDown, iconsSet, "down", colors, ColorType.KEY_BACKGROUND, keyIconColor);
        setIconKeyStyle(mBtnArrowRight, iconsSet, "right", colors, ColorType.KEY_BACKGROUND, keyIconColor);
        setIconKeyStyle(mBtnSpace, iconsSet, "space_key_for_number_layout", colors, ColorType.SPACE_BAR_BACKGROUND, keyIconColor);
    }

    private void setKeyStyle(TextView textView, Colors colors, ColorType bgType, int textColor) {
        textView.setBackground(createKeyBackground(colors, bgType));
        textView.setTextColor(textColor);
    }

    private void setIconKeyStyle(ImageView imageView, KeyboardIconsSet iconsSet, String iconName, Colors colors, ColorType bgType, int iconColor) {
        imageView.setBackground(createKeyBackground(colors, bgType));
        if (iconsSet != null) {
            Drawable icon = iconsSet.getIconDrawable(iconName);
            if (icon != null) {
                Drawable mutated = icon.mutate();
                mutated.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                imageView.setImageDrawable(mutated);
            }
        }
    }

    private Drawable createKeyBackground(Colors colors, ColorType colorType) {
        float density = getContext().getResources().getDisplayMetrics().density;
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(6f * density);
        gd.setColor(colors.get(colorType));
        return gd;
    }
}
