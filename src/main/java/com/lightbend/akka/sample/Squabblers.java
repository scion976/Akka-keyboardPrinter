package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.PoisonPill;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class Squabblers extends AbstractActor {

    private static AtomicInteger idx = new AtomicInteger(65);

    public static class Say {
        private final Number msgNum;
        Say(Number msgNum) {
            this.msgNum = msgNum;
        }
    }

    private Random random = new Random();
    private char index;
    private int lastSleep;
    private Squabblers() {
        this.index = (char)idx.getAndIncrement();
    }

    private void tired(final Number msgNumber) {
        CompletableFuture future = CompletableFuture.supplyAsync(() -> {
            try {
                int currSleep = (random.nextInt(3)) * 1000;
                Thread.sleep(currSleep);
                lastSleep = currSleep / 1000;
                return lastSleep;
            }
            catch (InterruptedException e) { return -1; }
        }).thenAcceptAsync(sleepTime -> {
            System.out.println(index + ": got message #" + msgNumber);
        }).exceptionally(e -> {
            System.out.println("FAILURE");
            self().tell(PoisonPill.getInstance(), self());
            return null;
        });

//        final ActorSystem system = context().system();
//        final ActorRef parent = context().parent();
//        final ExecutionContext ec = system.dispatcher();
//        Future<Integer> future = future(new Callable<Integer>() {
//            public Integer call() throws InterruptedException {
//                int i = (random.nextInt(10) + 1) * 1000;
//                Thread.sleep(i);
//                return i / 1000;
//            }
//        }, ec);
//
//        future.onComplete(f-> {
//            if(f.isSuccess()) {
//                parent.tell(new Whinge(index, f.get()), self());
//                tired();
//            }
//            else {
//                System.out.println("FAILURE");
//                self().tell(PoisonPill.getInstance(), self());
//            }
//            return f;
//        }, ec);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(Say.class, say -> tired(say.msgNum))
            .matchAny(f -> System.out.println("got " + f))
            .build();
    }
}
