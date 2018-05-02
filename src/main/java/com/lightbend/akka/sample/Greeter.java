package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.lightbend.akka.sample.Printer.Greeting;

//#greeter-messages
public class Greeter extends AbstractActor {
//#greeter-messages
  static public Props props(String message, ActorRef printerActor) {
    return Props.create(Greeter.class, () -> new Greeter(message, printerActor));
  }

  //#greeter-messages
  static public class WhoToGreet {
    public final String who;

    public WhoToGreet(String who) {
        this.who = who;
    }
  }

  static public class Greet {
    private String addendum = "";
    public Greet() {
    }
    public Greet(String addendum) {
        this.addendum = " " + addendum;
    }
  }
  //#greeter-messages

  private final String message;
  private final ActorRef printerActor;
  private String greeting = "";

  public Greeter(String message, ActorRef printerActor) {
    this.message = message;
    this.printerActor = printerActor;
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(WhoToGreet.class, wtg -> {
          this.greeting = message + ", " + wtg.who;
        })
        .match(Greet.class, greet -> {
          //#greeter-send-message
          printerActor.tell(new Greeting(greeting + greet.addendum), getSelf());
          //#greeter-send-message
        }).matchAny(x -> {
            System.out.println("Any");
            })
        .build();
  }
//#greeter-messages
}
//#greeter-messages
