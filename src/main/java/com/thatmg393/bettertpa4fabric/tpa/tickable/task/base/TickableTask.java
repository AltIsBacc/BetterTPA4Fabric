package com.thatmg393.bettertpa4fabric.tpa.tickable.task.base;

public abstract class TickableTask {
    protected enum TickResult {
        CONTINUE, PAUSE, CANCEL, RESET, REPEAT
    }

    private final long initialTickDuration;

    private long tickDuration;
    private boolean firstTick = true;

    public TickableTask(long tickDuration) {
        this.tickDuration = tickDuration;
        this.initialTickDuration = tickDuration;
    }

    public final boolean tick() {
        if (firstTick) {
            firstTick = false;
            onFirstTick();
            return true;
        }

        return switch (onTick()) {
            case CANCEL -> false;
            case PAUSE -> true;

            case RESET -> {
                tickDuration = initialTickDuration;
                firstTick = true;
                yield true;
            }

            case CONTINUE -> {
                if (tickDuration > 0) tickDuration--;
                if (tickDuration == 0) {
                    onFinish();
                    yield false;
                }

                yield true;
            }

            case REPEAT -> {
                if (tickDuration > 0) tickDuration--;
                if (tickDuration == 0) {
                    onFinish();
                    tickDuration = initialTickDuration;
                }

                yield true;
            }
        };
    }

    public long getInitialTickDuration() {
        return initialTickDuration;
    }

    public long getTickDuration() {
        return tickDuration;
    }

    protected void onFirstTick() { }

    protected abstract TickResult onTick(); 
    protected abstract void onFinish();
}
