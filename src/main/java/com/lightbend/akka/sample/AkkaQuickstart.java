package com.lightbend.akka.sample;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AllDeadLetters;
import com.lightbend.akka.sample.Greeter.WhoToGreet;
import com.lightbend.akka.sample.Greeter.Greet;

public class AkkaQuickstart {
    public static void main(String[] args) {

        final ActorSystem system = ActorSystem.create("helloakka");

        //#create-actors
        final ActorRef printerActor =
                system.actorOf(Printer.props(), "printerActor");
        final ActorRef howdyGreeter =
                system.actorOf(Greeter.props("Howdy", printerActor), "howdyGreeter");
        final ActorRef helloGreeter =
                system.actorOf(Greeter.props("Hello", printerActor), "helloGreeter");
        final ActorRef goodDayGreeter =
                system.actorOf(Greeter.props("Good day", printerActor), "goodDayGreeter");

        system.actorOf(MySupervisor.props(printerActor), "supervisor");
        system.eventStream().subscribe(system.actorOf(DivinePostman.props(printerActor)), AllDeadLetters.class);

        //#create-actors

        //#main-send-messages
//        reader.tell(new Start(), ActorRef.noSender());
        howdyGreeter.tell(new WhoToGreet("Akka"), ActorRef.noSender());
        howdyGreeter.tell(new Greet(), ActorRef.noSender());

        howdyGreeter.tell(new WhoToGreet("Lightbend"), ActorRef.noSender());
        howdyGreeter.tell(new Greet(), ActorRef.noSender());

        helloGreeter.tell(new WhoToGreet("Java"), ActorRef.noSender());
        helloGreeter.tell(new Greet(), ActorRef.noSender());

        goodDayGreeter.tell(new WhoToGreet("Play"), ActorRef.noSender());
        goodDayGreeter.tell(new Greet(), ActorRef.noSender());
    }
}
