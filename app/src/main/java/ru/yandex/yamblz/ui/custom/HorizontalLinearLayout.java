package ru.yandex.yamblz.ui.custom;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple layout which lays views using following rules:
 * 1) There can be any amount of wrap_content or fixed size width views but only one with match_parent width.
 * If there is more than one match_parent width view then {@link IllegalArgumentException} is thrown.
 * 2) Views are laid from left to right. Wrap_content and fixed size width views take as much place as they need,
 * match_parent width view takes not occupied space
 * 3) There is no constraints on height of views.
 * 4) For child views {@link android.view.ViewGroup.LayoutParams} are used.
 */
public class HorizontalLinearLayout extends ViewGroup {

    /**
     * Just temp object for setting coordinates
     */
    private final Rect tmpRect = new Rect();

    private final List<View> mMatchParentHeightViews = new ArrayList<View>(0);

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

        mMatchParentHeightViews.clear();

        final int count = getChildCount();

        //view with match_parent width
        View matchParentChild = null;
        //already filled horizontal space
        int filledHorizontalSpace = 0;

        int maxHeight = 0;
        int childState = 0;

        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int verticalPadding = getPaddingTop() + getPaddingBottom();

        //whether we need to measure match_parent height views after all the others
        final boolean needMeasureMPHeight = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;

        //first we measure all views which are not match_parent width and not match_parent height (if need)
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                //if view is gone do nothing
                continue;
            }
            LayoutParams layoutParams = child.getLayoutParams();

            if (needMeasureMPHeight && layoutParams.height == LayoutParams.MATCH_PARENT) {
                mMatchParentHeightViews.add(child);
                continue;
            }

            if (layoutParams.width != LayoutParams.MATCH_PARENT) {
                child.measure(
                        getChildWidthMeasureSpec(widthMeasureSpec, horizontalPadding,
                                filledHorizontalSpace, layoutParams.width),
                        getChildMeasureSpec(heightMeasureSpec, verticalPadding, layoutParams.height)
                );

                filledHorizontalSpace += child.getMeasuredWidth();
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
                childState = combineMeasuredStates(childState, child.getMeasuredState());
            } else if (matchParentChild == null) {
                //the view with match_parent will be measured after the others
                matchParentChild = child;
            } else {
                //it's not allowed to have more than one view with match_parent
                throw new IllegalArgumentException("More than one child with match_parent");
            }
        }
        if (!mMatchParentHeightViews.contains(matchParentChild)) {
            //if match_parent width view has not match_parent height then we need measure its height,
            //to define heights of match_parent height views
            LayoutParams layoutParams = matchParentChild.getLayoutParams();
            matchParentChild.measure(
                    getChildWidthMeasureSpec(widthMeasureSpec, horizontalPadding,
                            filledHorizontalSpace, layoutParams.width),
                    getChildMeasureSpec(heightMeasureSpec, verticalPadding, layoutParams.height)
            );
            maxHeight = Math.max(maxHeight, matchParentChild.getMeasuredHeight());
        }

        //now we know all heights of non match_parent height views, then measure them
        for (View view : mMatchParentHeightViews) {
            if (view == matchParentChild) {
                continue;
            }
            LayoutParams layoutParams = view.getLayoutParams();
            view.measure(
                    getChildWidthMeasureSpec(widthMeasureSpec, horizontalPadding,
                            filledHorizontalSpace, layoutParams.width),
                    MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY)
            );
            filledHorizontalSpace += view.getMeasuredWidth();
            childState = combineMeasuredStates(childState, view.getMeasuredState());
        }

        //measure match_parent child after all the others were measured
        if (matchParentChild != null) {
            LayoutParams layoutParams = matchParentChild.getLayoutParams();
            final int heightSpec = (mMatchParentHeightViews.contains(matchParentChild)) ?
                    MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY) :
                    getChildMeasureSpec(heightMeasureSpec, verticalPadding, layoutParams.height);

            matchParentChild.measure(
                    getChildWidthMeasureSpec(widthMeasureSpec, horizontalPadding, filledHorizontalSpace,
                            layoutParams.width),
                    heightSpec);
            filledHorizontalSpace += matchParentChild.getMeasuredWidth();
            childState = combineMeasuredStates(childState, matchParentChild.getMeasuredState());
        }
        setMeasuredDimension(resolveSizeAndState(filledHorizontalSpace, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight + verticalPadding, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int count = getChildCount();
        final int parentTop = getPaddingTop();
        int leftPos = getPaddingLeft();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
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

    /**
     * Makes specs for width of a child. Pretty much like {@link ViewGroup#getChildMeasureSpec(int, int, int)},
     * except that it checks already occupied space
     *
     * @param parentWidthMeasureSpec parent width spec
     * @param padding                parent's padding
     * @param filledHorizontalSpace  already filled horizontal space
     * @param childWidth             child desired width
     * @return specs
     */
    protected int getChildWidthMeasureSpec(int parentWidthMeasureSpec, int padding, int filledHorizontalSpace,
                                           int childWidth) {
        int resultSize = 0;
        int resultMode = 0;

        final int specMode = MeasureSpec.getMode(parentWidthMeasureSpec);
        final int specSize = MeasureSpec.getSize(parentWidthMeasureSpec);
        final int spaceLeft = Math.max(0, specSize - filledHorizontalSpace - padding);

        switch (specMode) {
            case MeasureSpec.EXACTLY:
                if (childWidth >= 0) {
                    resultSize = childWidth;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childWidth == LayoutParams.MATCH_PARENT) {
                    resultSize = spaceLeft;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childWidth == LayoutParams.WRAP_CONTENT) {
                    resultSize = spaceLeft;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;
            case MeasureSpec.AT_MOST:
                if (childWidth >= 0) {
                    resultSize = childWidth;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childWidth == LayoutParams.MATCH_PARENT) {
                    resultSize = spaceLeft;
                    resultMode = MeasureSpec.AT_MOST;
                } else if (childWidth == LayoutParams.WRAP_CONTENT) {
                    resultSize = spaceLeft;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;
            case MeasureSpec.UNSPECIFIED:
                if (childWidth >= 0) {
                    resultSize = childWidth;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childWidth == LayoutParams.MATCH_PARENT) {
                    resultSize = spaceLeft;
                    resultMode = MeasureSpec.UNSPECIFIED;
                } else if (childWidth == LayoutParams.WRAP_CONTENT) {
                    resultSize = spaceLeft;
                    resultMode = MeasureSpec.UNSPECIFIED;
                }
                break;
        }
        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }
}
