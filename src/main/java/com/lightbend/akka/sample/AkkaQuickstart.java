package com.lightbend.akka.sample;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AllDeadLetters;
import akka.actor.Props;
import akka.routing.FromConfig;
import com.lightbend.akka.sample.Greeter.WhoToGreet;
import com.lightbend.akka.sample.Greeter.Greet;
import com.lightbend.akka.sample.supervisor.MySupervisor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class AkkaQuickstart {
    public static void main(String[] args) {

        Config config = ConfigFactory.load();
        final ActorSystem system = ActorSystem.create("helloakka", config.getConfig("akka"));

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
        ActorRef router = system.actorOf(Props.create(Squabblers.class).withRouter(new FromConfig()), "router");
        ActorRef gatling = system.actorOf(Gatling.props(50, router, f -> new Squabblers.Say((Number) f)));
        system.registerOnTermination(new Runnable() {
            @Override
            public void run() {
                gatling.tell("end", ActorRef.noSender());
            }
        });

        system.eventStream().subscribe(system.actorOf(DivinePostman.props(printerActor)), AllDeadLetters.class);

        //#create-actors
        //#main-send-messages
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
