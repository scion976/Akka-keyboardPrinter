package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

//#printer-messages
public class Printer extends AbstractActor {
//#printer-messages
  static public Props props() {
    return Props.create(Printer.class, Printer::new);
  }

  //#printer-messages
  static public class Greeting {
    public final String message;

    public Greeting(String message) {
      this.message = message;
    }
  }
  //#printer-messages

  private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  @Override
  public Receive createReceive() {
    return receiveBuilder()
            .match(Greeting.class, greeting -> {
//                log.info(greeting.message);
                System.out.println(greeting.message);
            })
//            .matchAny(message -> log.info(message.toString()))
            .matchAny(message -> System.out.println(message.toString()))
        .build();
  }
//#printer-messages
}
//#printer-messages
