package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import scala.concurrent.ExecutionContext;

import java.util.Collection;
import java.util.Collections;
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
    private boolean loop = true;

    @Override
    public void preStart() throws Exception {
        System.out.println(akka.serialization.Serialization.serializedActorPath(self()));
        run();
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        loop = false;
        queue.clear();
        queue.offer(new ScheduleWakeUpCall<>(Collections.emptySet(), System.currentTimeMillis(), null));
    }

    private void run() {
        Futures.future(() -> {
            while (loop) {
                ScheduleWakeUpCall<?> nextCall = queue.take(); // blocks
                nextCall
                    .getWhoToNotify()
                    .forEach(next -> next.tell(new WakeUpCall<>(nextCall.getMessage()), self()));
            }
            return null;
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

    public static class ScheduleWakeUpCall<T> implements Delayed {
        private final Collection<ActorRef> whoToNotify;
        private final long whenToAwakeET;
        private final T message;

        // deliberately private, use static create methods
        private ScheduleWakeUpCall(Collection<ActorRef> whoToNotify, long whenToAwakeET, T message) {
            this.whoToNotify = whoToNotify;
            this.whenToAwakeET = whenToAwakeET;
            this.message = message;
        }

        public Collection<ActorRef> getWhoToNotify() {
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

        static<T> ScheduleWakeUpCall createWakeupCall(Collection<ActorRef> whoToNotify, long whenToAwakeET, T t) {
            return Optional.ofNullable(whoToNotify)
                    .map(toNotify -> new ScheduleWakeUpCall<>(toNotify, whenToAwakeET, t))
                    .orElse(empty);
        }

        static<T> ScheduleWakeUpCall createWakeupCall(ActorRef whoToNotify, long whenToAwakeET, T t) {
            return createWakeupCall(Collections.singleton(whoToNotify), whenToAwakeET, t);
        }

        static ScheduleWakeUpCall createWakeupCall(ActorRef whoToNotify, long whenToAwakeET) {
            return createWakeupCall(Collections.singleton(whoToNotify), whenToAwakeET, null);
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
