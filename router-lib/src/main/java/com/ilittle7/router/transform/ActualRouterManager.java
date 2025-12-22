package com.ilittle7.router.transform;


import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ilittle7.router.AbsRouterManager;
import com.ilittle7.router.RouterTree;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

public class ActualRouterManager extends AbsRouterManager {
    private ActualRouterManager(List var1, List var2, SortedMap var3, RouterTree var4) {
        super(var1, var2, var3, var4);
    }

    private static ArrayList globalInterceptorList = new ArrayList();
    private static ArrayList fallbackInterceptorList = new ArrayList();
    private static TreeMap sortedRouterMap = new TreeMap();
    private static RouterTree routerPathTree = RouterTree.emptyTree();

    public static ActualRouterManager create() {
        loadAll();
        return new ActualRouterManager(globalInterceptorList, fallbackInterceptorList, sortedRouterMap, routerPathTree);
    }

    public static void loadAll() {
    }

    public static void register(String className) {
        Log.i("ActualRouterManager", "register  className:" + className);
        if (!TextUtils.isEmpty(className)) {
            try {
                Class<?> objectClass = Class.forName(className);
                Field instanceField = objectClass.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                Object obj = instanceField.get(null);

                if (obj instanceof AbsRouterManager) {
                    AbsRouterManager manager = (AbsRouterManager) obj;
                    globalInterceptorList.addAll(manager.getGlobalInterceptorList());
                    fallbackInterceptorList.addAll(manager.getFallbackInterceptorList());
                    sortedRouterMap.putAll(manager.getSortedRouterMap());
                    routerPathTree.merge(manager.getRouterPathTree());
                    Log.i("ActualRouterManager", "register  success");
                }
            } catch (Exception e) {
                Log.i("ActualRouterManager", "register  error");
                e.printStackTrace();
            }
        }
    }

    private static final String pathParamKey = UUID.randomUUID().toString();
    private static final String postPathSegmentKey = UUID.randomUUID().toString();
    private static final String activityOptionKey = UUID.randomUUID().toString();
    private static final String dialogFragmentIntentKey = UUID.randomUUID().toString();

    @NonNull
    @Override
    public String getPathParamKey() {
        return pathParamKey;
    }

    @NonNull
    @Override
    public String getPostPathSegmentKey() {
        return postPathSegmentKey;
    }

    @NonNull
    @Override
    public String getActivityOptionKey() {
        return activityOptionKey;
    }

    @NonNull
    @Override
    public String getDialogFragmentIntentKey() {
        return dialogFragmentIntentKey;
    }
}
