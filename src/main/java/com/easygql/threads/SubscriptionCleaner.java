package com.easygql.threads;

import com.easygql.component.SubscriptionCacheService;

import java.util.concurrent.TimeUnit;
/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
public class SubscriptionCleaner implements Runnable{
    private static Long waittime=1L;//min

    /**
     *
     */
    @Override
    public void run() {
        while(true) {
            Long starttime=System.currentTimeMillis();
            try {
                TimeUnit.MINUTES.sleep(waittime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            SubscriptionCacheService.subscriptionCacheService.clearSubscription(starttime);
        }

    }
}
