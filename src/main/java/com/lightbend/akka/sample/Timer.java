package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import scala.concurrent.ExecutionContext;
import java.util.Optional;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class Timer extends AbstractActor {
    static public Props props() {
        return Props.create(Timer.class, Timer::new);
    }

    private static final ScheduleWakeUpCall empty = new ScheduleWakeUpCall<>(null, 0, null);

    private final DelayQueue<ScheduleWakeUpCall> queue = new DelayQueue<>();
    private final ExecutionContext ec = getContext().getSystem().dispatchers().lookup("blockingTimerDispatcher");

    @Override
    public void preStart() throws Exception {
        run();
    }

    private void run() {
        Futures.future(() -> {
            while (true) {
                ScheduleWakeUpCall next = queue.take(); // blocks
                next.getWhoToNotify().tell(new WakeUpCall<>(next.getMessage()), self());
            }
        }, ec);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(ScheduleWakeUpCall.class,
                call -> Optional.of(call)
                    .filter(ScheduleWakeUpCall::isValid)
                    .ifPresent(item -> queue.offer(item, item.lengthOfSleepFromNow(), TimeUnit.MILLISECONDS))
            )
            .build();
    }

    static class ScheduleWakeUpCall<T> implements Delayed {
        private final ActorRef whoToNotify;
        private final long whenToAwakeET;
        private final T message;

        // deliberately private, use static create methods
        private ScheduleWakeUpCall(ActorRef whoToNotify, long whenToAwakeET, T message) {
            this.whoToNotify = whoToNotify;
            this.whenToAwakeET = whenToAwakeET;
            this.message = message;
        }

        public ActorRef getWhoToNotify() {
            return whoToNotify;
        }

        public T getMessage() {
            return message;
        }

        boolean isValid() {
            return this != empty;
        }

        long lengthOfSleepFromNow() {
            return Math.max(whenToAwakeET - System.currentTimeMillis(), 0);
        }

        @Override
        public String toString() {
            return "Will wakeup " + whoToNotify + " in " + lengthOfSleepFromNow() + "ms with " + message;
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(lengthOfSleepFromNow(), TimeUnit.MILLISECONDS);
        }

        static<T> ScheduleWakeUpCall createWakeupCall(ActorRef whoToNotify, long whenToAwakeET, T t) {
            return Optional.ofNullable(whoToNotify)
                    .map(toNotify -> new ScheduleWakeUpCall<>(toNotify, whenToAwakeET, t))
                    .orElse(empty);
        }

        static ScheduleWakeUpCall createWakeupCall(ActorRef whoToNotify, long whenToAwakeET) {
            return createWakeupCall(whoToNotify, whenToAwakeET, null);
        }

    }

    static class WakeUpCall<T> {
        private final T message;

        WakeUpCall(T message) {
            this.message = message;
        }

        public T getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "WakeUpCall{" +
                    "message=" + message +
                    '}';
        }
    }
}
