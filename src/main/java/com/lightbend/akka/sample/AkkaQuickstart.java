package com.lightbend.akka.sample;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AllDeadLetters;
import akka.actor.PoisonPill;
import com.lightbend.akka.sample.Greeter.WhoToGreet;
import com.lightbend.akka.sample.supervisor.PrinterSupervisor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

//import kamon.Kamon;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

public class AkkaQuickstart {

    public static void main(String[] args) {
        Config config = ConfigFactory.load();
        final ActorSystem system = ActorSystem.create("helloakka", config.getConfig("akka"));

        //#create-actors
        final ActorRef printerActor = system.actorOf(Printer.props(), "printerActor");
        final ActorRef howdyGreeter = system.actorOf(Greeter.props("Howdy", printerActor), "howdyGreeter");
        final ActorRef helloGreeter = system.actorOf(Greeter.props("Hello", printerActor), "helloGreeter");
        final ActorRef goodDayGreeter = system.actorOf(Greeter.props("Good day", printerActor), "goodDayGreeter");

        system.actorOf(PrinterSupervisor.props(printerActor), "supervisor");
        system.eventStream().subscribe(system.actorOf(DivinePostman.props(printerActor)), AllDeadLetters.class);

        //#create-actors
        //#main-send-messages
        howdyGreeter.tell(new WhoToGreet("Akka"), ActorRef.noSender());
        howdyGreeter.tell(new WhoToGreet("Lightbend"), ActorRef.noSender());
        helloGreeter.tell(new WhoToGreet("Java"), ActorRef.noSender());
        goodDayGreeter.tell(new WhoToGreet("Play"), ActorRef.noSender());
        List<ActorRef> allGreeters = Arrays.asList(howdyGreeter, helloGreeter, goodDayGreeter);

        ActorRef timer = system.actorOf(Timer.props(), "timer");
        timer.tell(Timer.ScheduleWakeUpCall.createWakeupCall(printerActor, Duration.of(5, ChronoUnit.SECONDS), 5), ActorRef.noSender());
        timer.tell(Timer.ScheduleWakeUpCall.createWakeupCall(printerActor, Duration.of(1, ChronoUnit.SECONDS), 1), ActorRef.noSender());
        timer.tell(Timer.ScheduleWakeUpCall.createWakeupCall(printerActor, Duration.of(4, ChronoUnit.SECONDS), 4), ActorRef.noSender());
        timer.tell(Timer.ScheduleWakeUpCall.createWakeupCall(printerActor, Duration.of(3, ChronoUnit.SECONDS), 3), ActorRef.noSender());

        ActorRef wait = system.actorOf(Wait1Sec.props(allGreeters));
        system.registerOnTermination(() -> {
            wait.tell(PoisonPill.getInstance(), ActorRef.noSender());
        });
    }
}
