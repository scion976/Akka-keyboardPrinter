package com.lightbend.akka.sample.supervisor;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.japi.pf.DeciderBuilder;
import com.lightbend.akka.sample.Greeter;
import com.lightbend.akka.sample.SystemReader;
import scala.concurrent.duration.FiniteDuration;
import java.util.concurrent.TimeUnit;

public class PrinterSupervisor extends AbstractActor {
    static public Props props(ActorRef printerActor) {
        return Props.create(PrinterSupervisor.class, () -> new PrinterSupervisor(printerActor));
    }

    private static FiniteDuration finiteDuration = new FiniteDuration(5, TimeUnit.MILLISECONDS);
    private int numErrors = 0;
    private SupervisorStrategy strategy = new OneForOneStrategy(3, finiteDuration, DeciderBuilder.match(NullPointerException.class,
            e -> {
                numErrors++;
                if (numErrors == 1) {
                    System.out.println("supervisor resuming");
                    context().child("reader").get().tell(Props.empty(), self());

                    return SupervisorStrategy.resume();
                }
                else if(numErrors == 2) {
                    System.out.println("supervisor restarting");
                    return SupervisorStrategy.restart();
                }
                System.out.println("supervisor escalating");
                context().child("reader").get().tell(new Greeter.WhoToGreet("f"), self()); // deadLetter
                return SupervisorStrategy.escalate();
            }
    ).build());

    public PrinterSupervisor(ActorRef printerActor) {
        ActorRef reader = getContext().actorOf(SystemReader.props(printerActor), "reader");
        getContext().watch(reader);
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(Terminated.class, f -> {
                System.out.println(f + " was terminated");
                context().parent().tell(PoisonPill.getInstance(), self());
            })
            .matchAny(props -> System.out.println("I accept no messages")) //never called
        .build();
    }
}
