package com.galaxybruce.testlibrary;


import android.support.v4.content.FileProvider;

/**
 * @date 2019-06-24 19:55
 * @author bruce.zhang
 * @description
 * <p>
 * modification history:
 */
public class GFileProvider extends FileProvider {

    @Override
    public boolean onCreate() {
        // 可以在这里做些启动初始化工作
//        Utils.init(getContext());
        return true;
    }
}
