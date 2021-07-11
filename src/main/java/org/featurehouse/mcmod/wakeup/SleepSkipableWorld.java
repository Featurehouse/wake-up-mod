package org.featurehouse.mcmod.wakeup;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.SERVER)
//@FunctionalInterface
public interface SleepSkipableWorld {
    SleepManager wakeUp_getSleepManager();
    boolean wakeUp_isSleepingEnabled();
}
