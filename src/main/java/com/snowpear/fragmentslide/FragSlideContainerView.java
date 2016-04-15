package com.snowpear.fragmentslide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;

/**
 * Created by wangjinpeng on 15/11/2.
 * Fragment滑动的容器
 */
public class FragSlideContainerView extends RelativeLayout {
    public FragSlideContainerView(Context context) {
        super(context);
        init();
    }

    public FragSlideContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FragSlideContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private FragmentManager mFragmentManager;

    /**
     * 存储model的list
     */
    private ArrayList<FragSlideModel> mList;

    private void init(){
        mList = new ArrayList<FragSlideModel>();
    }

    public void setFragmentManager(FragmentManager fragmentManager){
        this.mFragmentManager = fragmentManager;
    }


    private float mFadePercent = 0f;
    private Drawable mShadowDrawable;
    private int mShadowWidth = 0;
    private float mScrollBehindScale = 0f;

    /**
     * 设置滑动中边缘的阴影及其宽度
     * @param shadow
     * @param shadowWidth
     */
    public void setShadowDrawable(Drawable shadow, int shadowWidth) {
        mShadowDrawable = shadow;
        mShadowWidth = shadowWidth;
    }
    /**
     * 设置第二层阴影的黑度，取值0-1，默认0
     * @param percent
     */
    public void setFadePercent(float percent){
        mFadePercent = percent;
    }
    /**
     * 设置第二层view的随动起始位置
     * @param scrollBehindScale 起始位置占view的比率 默认为0，取值范围 0~1
     */
    public void setScrollBehindScale(float scrollBehindScale) {
        this.mScrollBehindScale = scrollBehindScale;
    }

    /**
     * 设置回到背景的fragment是否需要从界面中移除，默认为false
     * @param mIsDetachBackFrag
     */
    public void setIsDetachBackFrag(boolean mIsDetachBackFrag) {
        this.mIsDetachBackFrag = mIsDetachBackFrag;
    }

    private boolean mIsDetachBackFrag = false;

    /**
     * 打开一个fragment
     * @param fragment
     */
    public void open(Fragment fragment){
        open(fragment, true);
    }


    /**
     * 打开一个fragment
     * @param fragment
     * @param isSmooth 是否平滑滑动
     */
    public void open(Fragment fragment, boolean isSmooth){
        //为空  或者 已经销毁 时不能创建页面
        if(mFragmentManager==null || mFragmentManager.isDestroyed()){
            return;
        }

        FragSlideModel firstModel = getModelByIndex(0);

        if(!FragSlideModel.isEmpty(firstModel)){
            if(firstModel.getView().isScrolling()){
                return;
            }
        }

        if(fragment!=null){
            int initX = 0;
            if(!FragSlideModel.isEmpty(firstModel)){
                firstModel.getView().setBehindView(null);
                firstModel.getView().setOnToggleChangedListener(null);
                initX = -getMeasuredWidth();
            }
            FragSlideChildView rl = new FragSlideChildView(getContext());
            rl.setId(rl.hashCode());
            addView(rl, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(rl.getId(), fragment);
            ft.commit();
            if(!FragSlideModel.isEmpty(firstModel)){
                rl.setBehindView(firstModel.getView());
            }else{
                rl.setBehindView(null);
            }
            rl.setOnToggleChangedListener(mTopViewToggleChangedListener);
            rl.setShadowDrawable(mShadowDrawable, mShadowWidth);//getResources().getDrawable(R.drawable.shadow_left)
            rl.setFadePercent(mFadePercent);
            rl.setScrollBehindScale(mScrollBehindScale);

            FragSlideModel newModel = FragSlideModel.create(rl, fragment);
            mList.add(0, newModel);

            if(!isSmooth){
                newModel.getView().enterFrom(0, 0, true);
            }else{
                newModel.getView().enterFrom(initX, 0);
            }

        }
    }

    /**
     * 返回一个界面
     * @param isSmooth
     * @return
     */
    public boolean back(boolean isSmooth){
        if(mFragmentManager==null || mFragmentManager.isDestroyed()){
            return false;
        }

        FragSlideModel firstModel = getModelByIndex(0);
        FragSlideModel secondModel = getModelByIndex(1);

        if(!FragSlideModel.isEmpty(firstModel)){
            if(firstModel.getView().isScrolling()){
                return false;
            }
        }

        if(!FragSlideModel.isEmpty(firstModel) && !FragSlideModel.isEmpty(secondModel)) {
            secondModel.getView().setVisibility(View.VISIBLE);
            if(mIsDetachBackFrag) {
                FragmentTransaction ft1 = mFragmentManager.beginTransaction();
                ft1.replace(secondModel.getView().getId(), secondModel.getFragment());
                ft1.commit();
            }
            if(isSmooth){
                firstModel.getView().exit();
            }
            return true;
        }
        return false;
    }

    /**
     * 顶层view的滑动回调
     */
    private FragSlideChildView.OnToggleChangedListener mTopViewToggleChangedListener = new FragSlideChildView.OnToggleChangedListener() {
        @Override
        public void onStartDrag() {
            back(false);
        }

        @Override
        public void onEndDrag() {

        }

        @Override
        public void onExitStart() {
            if(onPageChangedListener!=null){
                int size = getSize() - 1;
                onPageChangedListener.onStart(size - 1, size - 0, getFragmentByIndex(1));
            }
        }

        @Override
        public void onEnterStart() {
            if(onPageChangedListener!=null){
                int size = getSize() - 1;
                onPageChangedListener.onStart(size - 0, size - 1, getFragmentByIndex(0));
            }
        }

        @Override
        public void onExit() {
            endBack();
        }

        @Override
        public void onEnter(boolean isFirstOpen) {
            endOpen(isFirstOpen);
        }

        @Override
        public void onScrollProgress(View viewEnter, View viewExit, int progress) {
            int size = getSize() - 1;
            int posEnter = size - getIndexByView(viewEnter);
            int posExit = size - getIndexByView(viewExit);
            if(onPageChangedListener!=null){
                onPageChangedListener.onScrollProgress(posEnter, posExit, progress);
            }
        }
    };

    /**
     * 结束返回
     */
    private void endBack(){
        if(mFragmentManager==null || mFragmentManager.isDestroyed()){
            return;
        }

        FragSlideModel firstModel = getModelByIndex(0);
        FragSlideModel secondModel = getModelByIndex(1);
        FragSlideModel thirdModel = getModelByIndex(2);

        if(!FragSlideModel.isEmpty(firstModel)){
            FragmentTransaction ft2 = mFragmentManager.beginTransaction();
            ft2.remove(firstModel.getFragment());
            ft2.commit();
            firstModel.getView().setBehindView(null);
            firstModel.getView().setOnToggleChangedListener(null);
            removeView(firstModel.getView());
        }
        secondModel.getView().setOnToggleChangedListener(mTopViewToggleChangedListener);
        if(FragSlideModel.isEmpty(thirdModel)){
            secondModel.getView().setBehindView(null);
        }else{
            secondModel.getView().setBehindView(thirdModel.getView());
        }
        mList.remove(0);

        if(onPageChangedListener!=null){
            onPageChangedListener.onBackOver();
        }
    }

    /**
     * 结束打开
     */
    private void endOpen(boolean isFirstOpen){
        if(mFragmentManager==null || mFragmentManager.isDestroyed()){
            return;
        }
        FragSlideModel firstModel = getModelByIndex(0);
        FragSlideModel secondModel = getModelByIndex(1);

        if(!FragSlideModel.isEmpty(secondModel)) {
            if(mIsDetachBackFrag) {
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.remove(secondModel.getFragment());
            ft.commit();
            }
            secondModel.getView().setVisibility(View.GONE);
        }
        if(onPageChangedListener!=null){
            onPageChangedListener.onEnterOver(isFirstOpen);
        }
    }

    /**
     * 根据index取到model
     * @param index 索引
     * @return FragSlideModel 索引自上而下为从0增长
     */
    private FragSlideModel getModelByIndex(int index){
        if(index < mList.size()){
            return mList.get(index);
        }
        return null;
    }

    /**
     * 返回到已经打开的某一个界面
     * @param index 索引
     *              从打开的第一个界面计数为0，到当前的自增
     */
    public void backTo(int index){
        int size = mList.size();

        int backToIndex = size - 1 - index;
        if(backToIndex <= 0){
            return;
        }

        while ((mList.size() - 1 - 1)>index){
            FragSlideModel model = getModelByIndex(1);
            if(FragSlideModel.isEmpty(model)){
                break;
            }
            removeView(model.getView());
            model.recyle();
            mList.remove(1);
        }

        FragSlideModel firstModel = getModelByIndex(0);
        FragSlideModel secondModel = getModelByIndex(1);
        firstModel.getView().setBehindView(secondModel.getView());

        back(true);
    }

    /**
     * 获取Fragment
     * @param index
     * @return
     */
    public Fragment getFragmentByIndex(int index){
        FragSlideModel model = getModelByIndex(index);
        if(FragSlideModel.isEmpty(model)){
            return null;
        }else{
            return model.getFragment();
        }
    }

    /**
     * 根据索引获取view
     * @param index
     * @return
     */
    public ViewGroup getViewByIndex(int index){
        FragSlideModel model = getModelByIndex(index);
        if(FragSlideModel.isEmpty(model)){
            return null;
        }else {
            return model.getView();
        }
    }

    private int getIndexByView(View view){
        if(view!=null) {
            int size = getSize();
            for (int i = 0; i < size; i++) {
                FragSlideModel fsm = getModelByIndex(i);
                if (fsm != null && view.equals(fsm.getView())) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 获取界面中fragment的数量
     * @return
     */
    public int getSize(){
        return mList.size();
    }

    /**
     * 充值整个界面和fragment数组
     */
    public void resetLayoutList(){
        if(mFragmentManager == null || mFragmentManager.isDestroyed()){
            return;
        }
        for(FragSlideModel model : mList){
            if(!FragSlideModel.isEmpty(model)){
                FragmentTransaction ft = mFragmentManager.beginTransaction();
                ft.remove(model.getFragment());
                ft.commit();
            }
        }
        mList.clear();
        removeAllViews();
    }

    public interface OnPageChangedListener{
        void onStart(int enterPos, int exitPos, Fragment fragment);
        void onEnterOver(boolean isFirstOpen);
        void onBackOver();
        void onScrollProgress(int enterPos, int exitPos, int progress);
    }

    public void setOnPageChangedListener(OnPageChangedListener onPageChangedListener) {
        this.onPageChangedListener = onPageChangedListener;
    }

    private OnPageChangedListener onPageChangedListener;
}
