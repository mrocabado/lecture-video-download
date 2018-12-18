package com.barbri.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolUtil {

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static ExecutorService getExecutorService() {
        return executorService;
    }
}
