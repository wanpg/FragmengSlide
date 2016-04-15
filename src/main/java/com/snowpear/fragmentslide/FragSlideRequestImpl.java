package com.snowpear.fragmentslide;

/**
 * Created by wangjinpeng on 16/1/10.
 */
public interface FragSlideRequestImpl<I> {
    void onNewFragment(I data);
    void onBackFragment();
}
