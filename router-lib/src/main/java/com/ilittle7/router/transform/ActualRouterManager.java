package com.ilittle7.router.transform;

import android.util.Log;
import androidx.annotation.NonNull;
import com.ilittle7.router.AbsRouterManager;
import com.ilittle7.router.IRouterConfig;
import com.ilittle7.router.RouterTree;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
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
    private static String actualBaseUri = null;

    private static boolean loaded = false;

    public static ActualRouterManager create() {
        if (!loaded) {
            loadAll();
            loaded = true;
        }
        return new ActualRouterManager(globalInterceptorList, fallbackInterceptorList, sortedRouterMap, routerPathTree);
    }

    public static String getActualBaseUri() {
        if (!loaded) {
            loadAll();
            loaded = true;
        }
        return actualBaseUri;
    }

    /**
     * 使用 ServiceLoader 自动聚合所有模块的路由和配置
     */
    public static void loadAll() {
        try {
            // 1. 聚合路由表
            ServiceLoader<AbsRouterManager> loader = ServiceLoader.load(AbsRouterManager.class, AbsRouterManager.class.getClassLoader());
            for (AbsRouterManager manager : loader) {
                globalInterceptorList.addAll(manager.getGlobalInterceptorList());
                fallbackInterceptorList.addAll(manager.getFallbackInterceptorList());
                sortedRouterMap.putAll(manager.getSortedRouterMap());
                routerPathTree.merge(manager.getRouterPathTree());
            }

            // 2. 聚合全局配置 (BaseUri)
            ServiceLoader<IRouterConfig> configLoader = ServiceLoader.load(IRouterConfig.class, IRouterConfig.class.getClassLoader());
            for (IRouterConfig config : configLoader) {
                if (actualBaseUri == null && config.getBaseUri() != null) {
                    actualBaseUri = config.getBaseUri();
                }
            }
            Log.i("ActualRouterManager", "ServiceLoader aggregation completed");
        } catch (Exception e) {
            Log.e("ActualRouterManager", "ServiceLoader error", e);
        }
    }

    private static final String pathParamKey = UUID.randomUUID().toString();
    private static final String postPathSegmentKey = UUID.randomUUID().toString();
    private static final String activityOptionKey = UUID.randomUUID().toString();
    private static final String dialogFragmentIntentKey = UUID.randomUUID().toString();

    @NonNull
    @Override
    public String getPathParamKey() { return pathParamKey; }
    @NonNull
    @Override
    public String getPostPathSegmentKey() { return postPathSegmentKey; }
    @NonNull
    @Override
    public String getActivityOptionKey() { return activityOptionKey; }
    @NonNull
    @Override
    public String getDialogFragmentIntentKey() { return dialogFragmentIntentKey; }
}
