package com.sd.lib.utils.extend;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class FActivityObjectHolder
{
    private static final Map<Activity, FActivityObjectHolder> MAP_HOLDER = new WeakHashMap<>();

    private final WeakReference<Activity> mActivity;
    private final Map<Class<? extends Item>, Item> mItemHolder = new ConcurrentHashMap<>();

    private FActivityObjectHolder(Activity activity)
    {
        if (activity == null)
            throw new NullPointerException("activity is null");

        mActivity = new WeakReference<>(activity);

        if (!activity.isFinishing())
            activity.getApplication().registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
    }

    private Activity getActivity()
    {
        return mActivity.get();
    }

    /**
     * 获得某个Item
     *
     * @param clazz
     * @return
     */
    public synchronized <T extends Item> T getItem(Class<T> clazz)
    {
        if (clazz == null)
            throw new NullPointerException("clazz is null");

        if (clazz.isInterface())
            throw new IllegalArgumentException("clazz is interface " + clazz);

        if (Modifier.isAbstract(clazz.getModifiers()))
            throw new IllegalArgumentException("clazz is abstract " + clazz);

        Item item = mItemHolder.get(clazz);
        if (item == null)
        {
            item = createItem(clazz);

            final Activity activity = getActivity();
            if (activity != null && !activity.isFinishing())
            {
                mItemHolder.put(clazz, item);
                item.init(activity);
            }
        }
        return (T) item;
    }

    /**
     * 清空并销毁Item
     */
    private synchronized void clearItem()
    {
        for (Item item : mItemHolder.values())
        {
            item.destroy();
        }
        mItemHolder.clear();
    }

    private final Application.ActivityLifecycleCallbacks mActivityLifecycleCallbacks = new Application.ActivityLifecycleCallbacks()
    {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState)
        {
        }

        @Override
        public void onActivityStarted(Activity activity)
        {
        }

        @Override
        public void onActivityResumed(Activity activity)
        {
        }

        @Override
        public void onActivityPaused(Activity activity)
        {
        }

        @Override
        public void onActivityStopped(Activity activity)
        {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState)
        {
        }

        @Override
        public void onActivityDestroyed(Activity activity)
        {
            if (activity == getActivity())
            {
                activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                final FActivityObjectHolder holder = remove(activity);
                if (holder != null)
                    holder.clearItem();
            }
        }
    };

    public static synchronized FActivityObjectHolder of(Activity activity)
    {
        if (activity == null)
            return null;

        FActivityObjectHolder holder = MAP_HOLDER.get(activity);
        if (holder == null)
        {
            holder = new FActivityObjectHolder(activity);
            if (!activity.isFinishing())
                MAP_HOLDER.put(activity, holder);
        }
        return holder;
    }

    private static synchronized FActivityObjectHolder remove(Activity activity)
    {
        if (activity == null)
            return null;

        return MAP_HOLDER.remove(activity);
    }

    private static <T extends Item> T createItem(Class<T> clazz)
    {
        try
        {
            return clazz.newInstance();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        } catch (InstantiationException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public interface Item
    {
        /**
         * 初始化
         *
         * @param activity
         */
        void init(Activity activity);

        /**
         * 销毁
         */
        void destroy();
    }
}
