package com.lightbend.akka.sample;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class SystemReader extends AbstractActor {
    static public Props props(ActorRef printerActor) {
        return Props.create(SystemReader.class, () -> new SystemReader(printerActor));
    }

    //#greeter-messages
    private final InputStreamReader fileInputStream = new InputStreamReader(System.in);
    private final BufferedReader bufferedReader = new BufferedReader(fileInputStream);
    private final ActorRef printerActor;
    private int lineCounter = 0;

    public SystemReader(ActorRef printerActor) {
        this.printerActor = printerActor;
    }

    private final Supplier<String> supplier = () -> {
        try {
            return bufferedReader.readLine();
        } catch (IOException ee) {
            throw new RuntimeException(ee);
        }
    };

    @Override
    public void preStart() throws Exception {
        super.preStart();
        nonblockingRead();
    }

    private void nonblockingRead() {
        CompletableFuture.supplyAsync(supplier).thenAccept(fromKeyboard -> self().tell(fromKeyboard, self()));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(Props.class, ignore -> nonblockingRead())
            .match(String.class, fromKeyboard -> {
                if(fromKeyboard.isEmpty()) {
                    System.out.println("stopping");
                    self().tell(PoisonPill.getInstance(), self());
                }
                else if (fromKeyboard.equals("e")) {
                    System.out.println("exception");
                    throw new NullPointerException();
                }
                else {
                    printerActor.tell(new Printer.Greeting(lineCounter++ + ":" + fromKeyboard), self());
                    nonblockingRead();
                }

            })
            .matchAny(props -> System.out.println("I accept no messages")) //never called
            .build();
    }
}
