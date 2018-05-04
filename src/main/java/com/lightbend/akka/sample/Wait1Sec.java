package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import scala.concurrent.ExecutionContext;

import java.util.List;
import java.util.Random;

import static akka.dispatch.Futures.future;

public class Wait1Sec extends AbstractActor {
    static public Props props(List<ActorRef> allGreeters) {
        return Props.create(Wait1Sec.class, () -> new Wait1Sec(allGreeters));
    }

    private List<ActorRef> allGreeters;
    private Thread thread;
    private int count;
    private static final long waitTime = 1500;

    private Wait1Sec(List<ActorRef> allGreeters) {
        this.allGreeters = allGreeters;
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        ActorRef actorRef = allGreeters.get(new Random().nextInt(allGreeters.size()));
        actorRef.tell(new Greeter.Greet(Integer.valueOf(++count).toString()), self());
        go();
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        thread.interrupt();
    }

    private void go () {
        final ExecutionContext ec = context().dispatcher();
        future(() -> {
                thread = Thread.currentThread();
                Thread.sleep(waitTime);
                return allGreeters.get(new Random().nextInt(allGreeters.size()));
            }, ec)
        .onSuccess(
            new OnSuccess<ActorRef>() {
                @Override
                public void onSuccess(ActorRef actorRef) throws Throwable {
                    actorRef.tell(new Greeter.Greet(Integer.valueOf(++count).toString()), self());
                    go();
                }
            }, ec
        );
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .matchAny(props -> System.out.println("I accept no other types of messages: " + props)) //never called
            .build();
    }
}
