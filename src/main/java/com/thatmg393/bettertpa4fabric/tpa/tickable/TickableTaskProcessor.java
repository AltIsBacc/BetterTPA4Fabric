package com.thatmg393.bettertpa4fabric.tpa.tickable;

import com.thatmg393.bettertpa4fabric.tpa.tickable.task.base.TickableTask;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TickableTaskProcessor<T extends TickableTask> {
    private final ObjectArrayList<T> tasks = new ObjectArrayList<>();

    public void putTask(T task) {
        tasks.add(task);
    }

    public void removeTask(int index) {
        tasks.remove(index);
    }

    public void removeTask(T task) {
        tasks.remove(task);
    }

    public void doTick() {
        for (int i = tasks.size() - 1; i >= 0; i--) {
            if (!tasks.get(i).tick())
                tasks.remove(i);
        }
    }
}