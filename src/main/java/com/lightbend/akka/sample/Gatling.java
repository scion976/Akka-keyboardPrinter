package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class Gatling extends AbstractActor {
    private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private final AtomicInteger counter = new AtomicInteger();

    static public Props props(long delay, ActorRef victim, Function messageSupplier) {
        return Props.create(Gatling.class, () -> new Gatling(delay, victim, messageSupplier));
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        executor.shutdown();
    }

    private Gatling(long delay, ActorRef victim, Function messageSupplier) {
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    victim.tell(messageSupplier.apply(counter.getAndIncrement()), ActorRef.noSender());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 5000, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(m -> System.out.println("Unexpected message in Gatling: "+ m))
                .build();
    }
}
