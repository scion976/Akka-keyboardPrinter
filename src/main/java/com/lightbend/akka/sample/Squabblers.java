package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorIdentity;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Identify;
import akka.pattern.AskableActorSelection;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Squabblers extends AbstractActor {

    private static AtomicInteger nameDispenser = new AtomicInteger(65);

    private Random random = new Random();
    private char myName;
    private ActorRef timer;
    private Squabblers() {
        this.myName = (char) nameDispenser.getAndIncrement();
    }


    @Override
    public void preStart() throws Exception {
        super.preStart();
        try {
            ActorSelection sel = getContext().getSystem().actorSelection("/user/timer");
            AskableActorSelection asker = new AskableActorSelection(sel);
            Timeout t = new Timeout(5, TimeUnit.SECONDS);
            Future<Object> fut = asker.ask(new Identify(1), t);
            ActorIdentity ident = (ActorIdentity) Await.result(fut, t.duration());
            timer = ident.getRef();
            if(timer == null) {
                System.out.println(myName + " failed lookup");
            } else {
                System.out.println("successful lookup");
                tired(1);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void tired(long msgNumber) {
        int delayTime = (random.nextInt(5) + 1) * 1000;
        timer.tell(Timer.ScheduleWakeUpCall.createWakeupCall(
                context().parent(),
                System.currentTimeMillis() + delayTime,
                new Say(msgNumber, myName)
        ), self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(Timer.WakeUpCall.class, wakeUpCall -> {
                Say s = (Say) wakeUpCall.getMessage();
                System.out.println(myName + " got " + s.msgNum + " from " + s.getWhoSaidIt());
                tired(s.getMsgNum().longValue() + 1);
            })
            .matchAny(f -> System.out.println("got an unexpected message " + f))
            .build();
    }

    static class Say {
        private final long msgNum;
        private final char whoSaidIt;

        Say(long msgNum, char whoSaidIt) {
            this.msgNum = msgNum;
            this.whoSaidIt = whoSaidIt;
        }

        Number getMsgNum() {
            return msgNum;
        }

        char getWhoSaidIt() {
            return whoSaidIt;
        }
    }

}
