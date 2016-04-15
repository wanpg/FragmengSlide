package com.snowpear.fragmentslide;

import android.support.v4.app.Fragment;

/**
 * Created by wangjinpeng on 15/11/4.
 * 用于存储滚动过程中的View和fragment
 */
class FragSlideModel {

    private FragSlideChildView mView;
    private Fragment mFragment;

    public FragSlideChildView getView() {
        return mView;
    }

    public void setView(FragSlideChildView mView) {
        this.mView = mView;
    }

    public Fragment getFragment() {
        return mFragment;
    }

    public void setFragment(Fragment mFragment) {
        this.mFragment = mFragment;
    }


    public boolean isEmpty(){
        return mView == null || mFragment == null;
    }

    public void recyle(){
        mView = null;
        mFragment = null;
    }

    public static FragSlideModel create(FragSlideChildView view, Fragment fragment){
        FragSlideModel model = new FragSlideModel();
        model.mView = view;
        model.mFragment = fragment;
        return model;
    }

    public static boolean isEmpty(FragSlideModel model){
        return model==null || model.isEmpty();
    }
}
