package ru.yandex.yamblz.ui.custom;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class HorizontalLinearLayout extends ViewGroup {

    private final Rect tmpRect = new Rect();

    public HorizontalLinearLayout(Context context) {
        super(context);
    }

    public HorizontalLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int count = getChildCount();

        View matchParentChild = null;
        int filledHorizontalSpace = 0;
        int maxHeight = 0;
        int childState = 0;

        for(int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if(child.getVisibility() == View.GONE) {
                //if view is gone do nothing
                continue;
            }
            LayoutParams layoutParams = child.getLayoutParams();
            if(layoutParams.width != LayoutParams.MATCH_PARENT) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec, filledHorizontalSpace);
                filledHorizontalSpace += child.getMeasuredWidth();
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
                childState = combineMeasuredStates(childState, child.getMeasuredState());
            } else if(matchParentChild == null) {
                //the view with MATCH_PARENT will be measured after the others
                matchParentChild = child;
            } else {
                //it's not allowed to have more than one view with MATCH_PARENT
                throw new IllegalArgumentException("More than one child with MATCH_PARENT");
            }

        }

        if(matchParentChild != null) {
            measureChild(matchParentChild, widthMeasureSpec, heightMeasureSpec, filledHorizontalSpace);
            maxHeight = Math.max(maxHeight, matchParentChild.getMeasuredHeight());
            filledHorizontalSpace += matchParentChild.getMeasuredWidth();
        }

        setMeasuredDimension(resolveSizeAndState(filledHorizontalSpace, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec, childState));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int count = getChildCount();
        final int parentTop = getPaddingTop();

        int leftPos = getPaddingLeft();

        for(int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if(child.getVisibility() == View.GONE) {
                continue;
            }

            final int width = child.getMeasuredWidth();
            final int height = child.getMeasuredHeight();

            tmpRect.left = leftPos;
            tmpRect.right = leftPos + width;
            tmpRect.top = parentTop;
            tmpRect.bottom = parentTop + height;

            child.layout(tmpRect.left, tmpRect.top, tmpRect.right, tmpRect.bottom);

            leftPos = tmpRect.right;
        }
    }

    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec,
                                int filledHorizontalSpace) {
        child.measure(
                getChildWidthMeasureSpec(
                        parentWidthMeasureSpec, filledHorizontalSpace, child.getLayoutParams().width),
                getChildHeightMeasureSpec(
                        parentHeightMeasureSpec, child.getLayoutParams().height));
    }

    protected int getChildWidthMeasureSpec(int parentWidthMeasureSpec, int filledHorizontalSpace,
                                           int childWidth) {
        int resultSize = 0;
        int resultMode = 0;

        final int specMode = MeasureSpec.getMode(parentWidthMeasureSpec);
        final int specSize = MeasureSpec.getSize(parentWidthMeasureSpec);
        final int spaceLeft = Math.max(0, specSize - filledHorizontalSpace);

        switch (specMode) {
            case MeasureSpec.EXACTLY:
                if(childWidth >= 0) {
                    resultSize = childWidth;
                    resultMode = MeasureSpec.EXACTLY;
                } else if(childWidth == LayoutParams.MATCH_PARENT) {
                    resultSize = spaceLeft;
                    resultMode = MeasureSpec.EXACTLY;
                } else if(childWidth == LayoutParams.WRAP_CONTENT){
                    resultSize = spaceLeft;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;
            case MeasureSpec.AT_MOST:
                if(childWidth >= 0) {
                    resultSize = childWidth;
                    resultMode = MeasureSpec.EXACTLY;
                } else if(childWidth == LayoutParams.MATCH_PARENT) {
                    resultSize = spaceLeft;
                    resultMode = MeasureSpec.AT_MOST;
                } else if(childWidth == LayoutParams.WRAP_CONTENT) {
                    resultSize = spaceLeft;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;
            case MeasureSpec.UNSPECIFIED:
                if(childWidth >= 0) {
                    resultSize = childWidth;
                    resultMode = MeasureSpec.EXACTLY;
                } else if(childWidth == LayoutParams.MATCH_PARENT) {
                    resultSize = spaceLeft;
                    resultMode = MeasureSpec.UNSPECIFIED;
                } else if(childWidth == LayoutParams.WRAP_CONTENT) {
                    resultSize = spaceLeft;
                    resultMode = MeasureSpec.UNSPECIFIED;
                }
                break;
        }
        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }

    protected int getChildHeightMeasureSpec(int parentHeightMeasureSpec, int childHeight) {
        return getChildMeasureSpec(parentHeightMeasureSpec, 0, childHeight);
    }
}
