package com.gdx.cellular.elements;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.lang.reflect.Proxy;

/**
 * Allows core-only JUnit physics tests to initialize material colors without
 * an Android activity or desktop window. The real game supplies Gdx.files.
 * Tests intentionally use an empty optional texture directory.
 */
public final class PhysicsTestGdx {
    private PhysicsTestGdx() { }

    public static synchronized void installAssetStub() {
        if (Gdx.files != null) return;
        Gdx.files = (Files) Proxy.newProxyInstance(
                Files.class.getClassLoader(),
                new Class<?>[]{Files.class},
                (proxy, method, args) -> {
                    if (FileHandle.class.isAssignableFrom(method.getReturnType())) {
                        return new FileHandle("elementum-test-no-assets/"
                                + (args != null && args.length > 0 ? args[0] : ""));
                    }
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == long.class) return 0L;
                    if (method.getReturnType() == int.class) return 0;
                    return null;
                });
    }
}
