//package com.lightbend.akka.sample.router;
//
//import akka.actor.AbstractActor;
//import akka.actor.ActorRef;
//import akka.actor.PoisonPill;
//import akka.actor.Props;
//import akka.actor.Terminated;
//import akka.routing.ActorRefRoutee;
//import akka.routing.RoundRobinRoutingLogic;
//import akka.routing.Routee;
//import akka.routing.Router;
//import com.lightbend.akka.sample.Squabblers;
//
//import java.util.List;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
//public class MyRouter extends AbstractActor {
//    static public Props props() {
//        return Props.create(MyRouter.class, () -> new MyRouter());
//    }
//
//    private Router router;
//    {
////        List<Routee> routees = IntStream.rangeClosed(65,69)
////            .mapToObj(i -> getContext().actorOf(Squabblers.props((char)i)))
////            .peek(actor -> getContext().watch(actor))
////            .map(ActorRefRoutee::new)
////            .collect(Collectors.toList());
////        router = new Router(new RoundRobinRoutingLogic(), routees);
//    }
//
//    @Override
//    public void postStop() throws Exception {
//        super.postStop();
//        router.routees().toStream().foreach(r -> {
//            r.send(PoisonPill.getInstance(), ActorRef.noSender());
//            return r;
//        });
//    }
//
//    @Override
//    public Receive createReceive() {
//        return receiveBuilder()
//            .match(Terminated.class, message -> {
//                System.out.println("SOMEONE DIED");
//                //                router = router.removeRoutee(message.actor());
//                //                ActorRef r = getContext().actorOf(Props.create(Squabblers.class));
//                //                getContext().watch(r);
//                //                router = router.addRoutee(new ActorRefRoutee(r));
//            })
//            .matchAny(message -> {
//                router.route(message, getSender());
//            })
//            .build();
//    }
//}
