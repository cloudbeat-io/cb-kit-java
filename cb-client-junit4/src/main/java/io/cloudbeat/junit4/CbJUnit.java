package io.cloudbeat.junit4;

import io.cloudbeat.common.CbTestContext;

public final class CbJUnit {
    private static volatile CbJUnit instance;
    private CbTestContext ctx;

    public static CbJUnit getInstance() {
        if (instance == null) {
            synchronized (CbJUnit.class) {
                if (instance == null) {
                    instance = new CbJUnit();
                }
            }
        }

        return instance;
    }

    private CbJUnit() {
        ctx = new CbTestContext();
    }
}
