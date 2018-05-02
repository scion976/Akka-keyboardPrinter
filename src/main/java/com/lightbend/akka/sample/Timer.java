package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import scala.concurrent.ExecutionContext;

import java.time.Duration;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

public class Timer extends AbstractActor {
    static public Props props() {
        return Props.create(Timer.class, Timer::new);
    }

    private static final ScheduleWakeUpCall empty = new ScheduleWakeUpCall<>(null, 0, null);

    private NavigableSet<ScheduleWakeUpCall> next = new TreeSet<>();
    private ExecutionContext ec = getContext().getSystem().dispatchers().lookup("blockingTimerDispatcher");
    private Thread thread = Thread.currentThread();
    private ScheduleWakeUpCall current = empty;

    private synchronized void start() {
        System.out.println("started");
        if(!current.isValid()) {
            Optional.ofNullable(next.pollFirst()).ifPresent(this::run);
        }
        else if(current.lengthOfSleepFromNow() > Optional.ofNullable(next.first()).orElse(empty).lengthOfSleepFromNow()) {
            System.out.println("interrupted!!");
            thread.interrupt();
            if(current.isValid()) {
                ScheduleWakeUpCall newNext = next.pollFirst();
                next.add(current); // slight chance of a double notify
                System.out.println("removing " + current.notifyTime + " in favour of " + newNext.notifyTime);
                run(newNext);
            }
        } else {
            System.out.println("No run");
        }
    }

    private void run(final ScheduleWakeUpCall wakeUpCall) {
        current = wakeUpCall;
        Futures.future(() -> {
            thread = Thread.currentThread();
            try {
                Thread.sleep(wakeUpCall.lengthOfSleepFromNow());
                wakeUpCall.getWhoToNotify().tell(new WakeUpCall<>(wakeUpCall.getMessage()), self());
                current = empty;
                start();
            } catch (InterruptedException e) {
                System.out.println("Interrupted Exception");
            }
            return wakeUpCall;
        }, ec);
        System.out.println("does this block");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(ScheduleWakeUpCall.class, call -> {
                Optional.of(call)
                    .filter(ScheduleWakeUpCall::isValid)
                    .ifPresent(next::add);
                start();
            })
            .build();
    }

    static class ScheduleWakeUpCall<T> implements Comparable<ScheduleWakeUpCall> {
        private final ActorRef whoToNotify;
        private final long notifyTime;
        private final T message;

        public ActorRef getWhoToNotify() {
            return whoToNotify;
        }

        public T getMessage() {
            return message;
        }

        private ScheduleWakeUpCall(ActorRef whoToNotify, long notifyTime, T message) {
            this.whoToNotify = whoToNotify;
            this.notifyTime = notifyTime;
            this.message = message;
        }

        boolean isValid() {
            return this != empty;
        }

        long lengthOfSleepFromNow() {
            return Math.max(notifyTime - System.currentTimeMillis(), 0);
        }

        @Override
        public String toString() {
            return "Will awake " + whoToNotify + " in " + notifyTime;
        }

        @Override
        public int compareTo(ScheduleWakeUpCall o) {
            return Long.compare(notifyTime, o.notifyTime);
        }

        static<T> ScheduleWakeUpCall createWakeupCall(ActorRef whoToNotify, Duration duration, T t) {
            if(whoToNotify != null && duration != null && !duration.isNegative()) {
                return new ScheduleWakeUpCall<>(whoToNotify, System.currentTimeMillis() + duration.toMillis(), t);
            }
            return empty;
        }
        static ScheduleWakeUpCall createWakeupCall(ActorRef whoToNotify, Duration duration) {
            return createWakeupCall(whoToNotify, duration, null);
        }
    }

    static class WakeUpCall<T> {
        private final T message;

        WakeUpCall(T obj) {
            this.message = obj;
        }

        public T getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return message.toString();
        }
    }
}
