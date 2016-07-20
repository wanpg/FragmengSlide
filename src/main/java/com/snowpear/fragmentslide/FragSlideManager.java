package com.snowpear.fragmentslide;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.ViewGroup;

import com.snowpear.fragmentslide.R;

import java.util.ArrayList;

/**
 * Created by wangjinpeng on 16/1/8.
 * fragment切换的抽象类，泛型提供数据和fragment类
 */
public abstract class FragSlideManager<F extends Fragment, I> {

    private static final String TAG = FragSlideManager.class.getSimpleName();

    /**
     * 根据Data创建Fragment
     * @param position
     * @param data
     * @return
     */
    protected abstract F instantiateFragment(int position, I data);

    /**
     * 结束滑动
     * @param page
     */
    protected abstract void scrollEnd(int page, I data, boolean isPageChanged);

    /**
     * 开始滑动
     * @param from
     * @param to
     */
    protected abstract void scrollStart(int from, int to, I toData);

    /**
     * 页面跳转进度
     * @param from
     * @param to
     * @param progress
     */
    protected abstract void scrolling(int from, int to, int progress);



    /**
     * 由子类实现类型对比
     * @param data1
     * @param data2
     */
    protected abstract boolean equalData(I data1, I data2);

    /**
     * 通知到后台的fragment已经进入后台
     */
    protected abstract void noticeFragmentToBackground(F fragment);

    protected abstract I getPageDatabyFragment(F fragment);

    protected abstract boolean isDataValid(I data);

    private FragmentActivity mActivity;
    private FragSlideContainerView mContainerView;

    public FragSlideManager() {

    }

    public void init(FragmentActivity activity){
        init(activity, new FragSlideContainerView(activity));
    }

    public void init(FragmentActivity activity, FragSlideContainerView containerView){
        this.mActivity = activity;
        this.mContainerView = containerView;
        mContainerView.setScrollBehindScale(0.5f);
        mContainerView.setFadePercent(0.2f);
        mContainerView.setShadowDrawable(activity.getResources().getDrawable(R.drawable.shadow_left), 20);
        mContainerView.setFragmentManager(mActivity.getSupportFragmentManager());
        mContainerView.setOnPageChangedListener(new FragSlideContainerView.OnPageChangedListener() {

            @Override
            public void onStart(int enterPos, int exitPos, Fragment fragment) {
                I i = null;
                if (fragment != null) {
                    i = getPageDatabyFragment((F) fragment);
                }
                scrollStart(exitPos, enterPos, i);
            }

            @Override
            public void onEnterOver(boolean isFirstOpen) {
                finishedScroll(isFirstOpen);
                //此处去判断第二层fragment 并让他执行onToBackground的方法
                noticeFragmentToBackground(getFragmentByIndex(1));
            }

            @Override
            public void onBackOver() {
                finishedScroll(true);
            }

            @Override
            public void onScrollProgress(int enterPos, int exitPos, int progress) {
                scrolling(exitPos, enterPos, progress);
            }

        });
    }

    /**
     * 获取当前显示Fragment
     *
     * @return
     */
    public F getCurrentFragment() {
        return getFragmentByIndex(0);
    }

    public I getCurrentPageData() {
        F f = getCurrentFragment();
        if(f!=null){
            return getPageDatabyFragment(f);
        }
        return null;
    }

    private F getFragmentByIndex(int index) {
        Fragment fragment = mContainerView.getFragmentByIndex(index);
        if(fragment==null){
            return null;
        }
        return (F) fragment;
    }

    public ViewGroup getContainerView() {
        return mContainerView;
    }

    /**
     * 添加一个元素
     * @param position 从0开始
     * @param data, 需要添加的元素
     * @param isNeedAnim 是否需要动画
     */
    protected void addFragmentToPosition(int position, I data, boolean isNeedAnim) {
        Log.d("FileFragManager", "addFragmentToPosition--positon:" + position);
        mContainerView.open(instantiateFragment(position, data), isNeedAnim);
    }

    public void finishedScroll(boolean isPageChanged) {
        ArrayList<I> fileStack = getCurrentStack();
        if (fileStack.size() > 0) {
            int position = fileStack.size() - 1;
            scrollEnd(position, fileStack.get(position), isPageChanged);
        }
    }

    /**
     * 获取当前文件栈信息
     *
     * @return
     */
    public ArrayList<F> getFragmentStack() {
        int pageSize = mContainerView.getSize();
        ArrayList<F> list = new ArrayList<F>();
        for(int i=pageSize-1;i>=0;i--){
            list.add(getFragmentByIndex(i));
        }
        return list;
    }

    /**
     * 获取当前管理器中有多少个view
     * @return
     */
    public int getSize(){
        return mContainerView.getSize();
    }

    /**
     * 获取当前文件栈信息
     *
     * @return
     */
    public ArrayList<I> getCurrentStack() {
        int pageSize = mContainerView.getSize();
        ArrayList<I> list = new ArrayList<I>();
        for(int i=pageSize-1;i>=0;i--){
            list.add(getPageDatabyFragment(getFragmentByIndex(i)));
        }
        return list;
    }

    /**
     * 获取index位置的数据
     * @param index  从0开始
     * @return
     */
    public I getDataByIndex(int index) {
        int pageSize = mContainerView.getSize();
        F f = getFragmentByIndex(pageSize - 1 - index);
        if (f != null) {
            return getPageDatabyFragment(f);
        }
        return null;
    }

    /**
     * 更新目录信息
     *
     * @param datas
     * @param manager
     */
    public void updateDateSet(ArrayList<I> datas, FragmentManager manager) {
        mContainerView.setFragmentManager(manager);
        if(datas==null || datas.size()<=0 || datas.get(0) == null || !isDataValid(datas.get(0))){
            return;
        }
        ArrayList<I> fileStack = getCurrentStack();
        if (fileStack == null || fileStack.size() == 0) {
            fileStack = datas;
            mContainerView.resetLayoutList();
            int fileSize = fileStack.size();
            for (int i=0; i<fileSize; i++) {
                addFragmentToPosition(i, datas.get(i), i >= fileSize ? true : false);
            }
        } else {
            showItems(datas);
        }
    }


    /**
     * 更新显示内容
     *
     * @param data
     */
    public void showItem(I data) {
        showItem(data, false);
    }

    /**
     * 更新显示内容
     * @param data
     * @param isCreateNew  此参数如果为True，则一直打开新页面
     */
    public void showItem(I data, boolean isCreateNew) {
        ArrayList<I> fileStack = getCurrentStack();
        int index = indexOfArray(fileStack, data);
        ArrayList<I> newArray = new ArrayList<>();
        if (index != -1 && !isCreateNew) {
            I currentData = fileStack.get(fileStack.size() - 1);
            if (equalData(currentData, data)) {
                return;
            }
            for (int i = 0; i <= index; i++) {
                newArray.add(fileStack.get(i));
            }
        } else {
            newArray.addAll(fileStack);
            newArray.add(data);
        }
        showItems(newArray);
    }

    /**
     * 更新显示内容
     *
     * @param arrays 需要显示的目录
     */
    public void showItems(ArrayList<I> arrays) {

        ArrayList<I> fileStack = getCurrentStack();
        int compere = compereDirector(fileStack, arrays);
        if (compere == 0) {// 当前页面不用更新，
            int fileStackSize = fileStack.size();
            scrollEnd(fileStackSize - 1, fileStack.get(fileStackSize - 1), true);
            return;
        }else if (compere == -1) {// 当前页面所有元素均需要替换
            fileStack = arrays;
            mContainerView.resetLayoutList();
            int fileSize = fileStack.size();
            for (int i=0; i<fileSize; i++) {
                addFragmentToPosition(i, fileStack.get(i), i >= fileSize ? true : false);
            }
        }else if (compere == 1) {
            // 末尾新增元素  需要动画
            fileStack = arrays;
            int fileStackSize = fileStack.size();
            addFragmentToPosition(fileStackSize-1, fileStack.get(fileStackSize - 1), true);
        }else if(compere == 2 || compere == 3){
            //此处计算回滚的位置
            mContainerView.backTo(arrays.size()-1);
        }
    }


    /**
     * 比较目标目录和当前目录是否一致
     *
     * @return -1：array2替换array1，
     * 1：对array1末尾新增1个元素，
     * 2：对array1末尾删除1个元素
     * 3: 对array1退出指某个元素
     * 0：equals
     */
    protected int compereDirector(ArrayList<I> oldArray, ArrayList<I> newArray) {
        int array1Size = oldArray.size();
        int array2Size = newArray.size();
        if (array1Size == array2Size) {
            for (int i = 0; i < array1Size; i++) {
                if (!equalData(oldArray.get(i), newArray.get(i))) {
                    return -1;
                }
            }
            return 0;
        } else if (array1Size < array2Size && Math.abs(array1Size - array2Size) != 1) {
            return -1;
        } else if (array1Size < array2Size) {
            for (int i = 0; i < array1Size; i++) {
                if (!equalData(oldArray.get(i), newArray.get(i))) {
                    return -1;
                }
            }
            return 1;
        } else if (array1Size > array2Size) {
            for (int i = 0; i < array2Size; i++) {
                if (!equalData(oldArray.get(i), newArray.get(i))) {
                    return -1;
                }
            }
            return array1Size - array2Size > 1 ? 3 : 2;
        }
        return 0;
    }


    protected int indexOfArray(ArrayList<I> list, I data){
        if(list == null || list.size() <= 0){
            return -1;
        }
        int size = list.size();
        for(int i = size - 1; i >= 0; i--){
            I dataTmp = list.get(i);
            if(equalData(data, dataTmp)){
                return i;
            }
        }
        return -1;
    }
}
