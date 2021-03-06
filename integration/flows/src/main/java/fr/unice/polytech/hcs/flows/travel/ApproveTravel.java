package fr.unice.polytech.hcs.flows.travel;

import fr.unice.polytech.hcs.flows.expense.Status;
import fr.unice.polytech.hcs.flows.expense.Travel;
import fr.unice.polytech.hcs.flows.utils.Endpoints;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import java.util.HashMap;
import java.util.Map;

import static fr.unice.polytech.hcs.flows.utils.Endpoints.*;

public class ApproveTravel extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        restConfiguration()
                .component("servlet")
        ;

        rest("/travel").consumes("application/json").produces("application/json")
                .put("/{travelId}")
                .to(END_TRAVEL)
        ;

        from(END_TRAVEL)
                .routeId("approve-travel")
                .routeDescription("End a travel and check refund")

                .log("[" + END_TRAVEL + "] Remove shitty headers (thx camel)")
                .removeHeaders("CamelHttp*")

                .log("[" + END_TRAVEL + "] Convert to Travel Request")
                .process(e -> e.getIn().setBody(new TravelRequest(e.getIn().getHeader("travelId", String.class))))

                .log("[" + END_TRAVEL + "] Load travel")
                .inOut(GET_TRAVEL)

                .log("[" + END_TRAVEL + "] OUT : ${body}")

                .choice().when(simple("${body} == null"))
                    .process(e -> e.getIn().setBody(null))
                    .to(NOT_FOUND)
                    .stop()
                .end()

                .log("[" + END_TRAVEL + "] Sum expenses")
                .process(sumExpensesTravel)

                .log("[" + END_TRAVEL + "] Get travel destination")
                .inOut(GET_DESTINATION)

                .log("[" + END_TRAVEL + "] Check automatic refund")
                .process(checkAutomaticRefund)

                .log("[" + END_TRAVEL + "] Extract travel")
                .process(e -> {
                    Approval approval = e.getIn().getBody(Approval.class);
                    e.getIn().setBody(approval.travel);
                })

                .choice()
                    .when(simple("${header.autoRefund} == true"))
                        .log("[" + END_TRAVEL + "] Automatic refund")
                        .inOut(ACCEPT_REFUND)
                    .otherwise()
                        .log("[" + END_TRAVEL + "] Manual refund")
                        .inOut(MANUAL_REFUND)
                .end()

                .marshal().json(JsonLibrary.Jackson)
        ;

        from(ACCEPT_REFUND)
                .routeId("automatic-refund")
                .routeDescription("Automatic refund")

                .log("[" + ACCEPT_REFUND + "] Update travel status to Accepted")
                .process(e -> e.getIn().getBody(Travel.class).status = Status.REFUND_ACCEPTED)

                .log("[" + ACCEPT_REFUND + "] Save travel")
                .to(UPDATE_TRAVEL)

                .log("[" + ACCEPT_REFUND + "] Refund accepted")
                .to(REFUND_SENDING)

                .process(e -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "ok");
                    response.put("message", "Refund accepted.");

                    e.getIn().setBody(response);
                })
        ;

        from(MANUAL_REFUND)
                .routeId("manual-refund")
                .routeDescription("Manual refund")

                .log("[" + MANUAL_REFUND + "] Update travel status to Waiting")
                .process(e -> e.getIn().getBody(Travel.class).status = Status.WAITING_FOR_EXPLANATION)

                .log("[" + MANUAL_REFUND + "] Save travel")
                .to(UPDATE_TRAVEL)

                .process(e -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "pending");
                    response.put("message", "You need to justify your budget overrun.");

                    e.getIn().setBody(response);
                })
        ;

        from(Endpoints.REFUSE_REFUND)
                .routeId("refuse-refund")
                .routeDescription("Refuse refund")

                .log("[" + REFUSE_REFUND + "] Update travel status to REFUSED")
                .process(e -> e.getIn().getBody(Travel.class).status = Status.REFUND_REFUSED)

                .log("[" + REFUSE_REFUND + "] Save travel")
                .to(UPDATE_TRAVEL)

                .process(e -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "refused");
                    response.put("message", "Refund refused.");

                    e.getIn().setBody(response);
                })
        ;

        from(GET_DESTINATION)
                .routeId("get-destination")
                .routeDescription("Get destination travel")

                .log("[" + GET_DESTINATION + "] Get travel destination")
                .process(e -> {
                    Approval approval = e.getIn().getBody(Approval.class);

                    // mock get destination
                    approval.destination = "Nice";
                })
        ;
    }

    private static Processor sumExpensesTravel = (exchange -> {
        Travel travel = exchange.getIn().getBody(Travel.class);

        // compute sum of expenses
        Double sum = travel.documents.stream().mapToDouble(expense -> expense.price).sum();

        // create approval object
        Approval approval = new Approval();
        approval.travel = travel;
        approval.sum = sum;

        exchange.getIn().setBody(approval);
    });

    private static Processor checkAutomaticRefund = (exchange -> {
        Approval approval = exchange.getIn().getBody(Approval.class);

        // default refuse automatic refund TODO
        manualRefund(exchange);

        if (approval.destination.equalsIgnoreCase("Nice") && approval.sum < 2000) {
            automaticRefund(exchange);
        }
    });

    private static void automaticRefund(Exchange exchange) {
        exchange.getIn().setHeader("autoRefund", true);
    }

    private static void manualRefund(Exchange exchange) {
        exchange.getIn().setHeader("autoRefund", false);
    }
}
