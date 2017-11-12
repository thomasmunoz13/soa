package fr.unice.polytech.hcs.flows.explanation;

import fr.unice.polytech.hcs.flows.travel.TravelRequest;
import fr.unice.polytech.hcs.flows.utils.Endpoints;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import java.util.Map;

import static fr.unice.polytech.hcs.flows.utils.Endpoints.*;

public class ExplanationProvider extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        restConfiguration()
                .component("servlet")
        ;

        rest("/explanation")
                .post().to(EXPLANATION_PROVIDER)
                .put().to(EXPLANATION_ANSWER)
        ;


        // Explanation for why did you use so much money dude.
        from(EXPLANATION_PROVIDER)
                .routeId("explanation-provider")

                .log("[" + EXPLANATION_PROVIDER + "] Received explanation")

                .log("[" + EXPLANATION_PROVIDER + "] Remove shitty headers (thx camel)")
                .removeHeaders("CamelHttp*")

                .log("[" + EXPLANATION_PROVIDER + "] Unmarshal explanation from JSON to POJO")
                .unmarshal().json(JsonLibrary.Jackson, Explanation.class)

                .log("[" + EXPLANATION_PROVIDER + "] Prepare request parameters for DB search route")
                .process(e -> {
                    Explanation explanation = e.getIn().getBody(Explanation.class);

                    // Set search criterion (object id from mongodb)
                    TravelRequest travelRequest   = new TravelRequest();
                    travelRequest.travelId = explanation.id;
                    e.getIn().setBody(travelRequest);
                    // Save the explanation for the manager to be able to review it
                    e.getIn().setHeader("explanation", explanation.explanation);
                })

                .log("[" + EXPLANATION_PROVIDER + "] Send to DB search route")
                .inOut(GET_TRAVEL_DB_OBJECT)
                .choice().when(simple("${body} == null"))
                .process(e -> e.getIn().setBody(null))
                .to(NOT_FOUND)
                .stop()
                .end()

                .log("[" + EXPLANATION_PROVIDER + "] Received DB response, parsing to map")
                .process(exchange -> {
                    Map body = exchange.getIn().getBody(Map.class);
                    body.put("explanation", exchange.getIn().getHeader("explanation"));
                    exchange.getIn().setBody(body);
                })

                .log("[" + EXPLANATION_PROVIDER + "] Sending new updated object to DB")
                .to(SAVE_TRAVEL_DATABASE_EP)
        ;


        from(EXPLANATION_ANSWER)
                // Initial stuff
                .routeId("explanation-answer")
                .routeDescription("answer to an explanation.")

                .log("[" + EXPLANATION_ANSWER + "] Received answer")

                .log("[" + EXPLANATION_ANSWER + "] Remove shitty headers (thx camel)")
                .removeHeaders("CamelHttp*")

                .log("[" + EXPLANATION_ANSWER + "] Unmarshal explanation answer from JSON to POJO: ${body}")
                .unmarshal().json(JsonLibrary.Jackson, ExplanationAnswer.class)

                .log("[" + EXPLANATION_ANSWER + "] Prepare request parameters for DB search travel")
                .process(e -> {
                    ExplanationAnswer explanationAnswer = e.getIn().getBody(ExplanationAnswer.class);
                    e.getIn().setBody(new TravelRequest(explanationAnswer.travelId));
                    e.getIn().setHeader("acceptRefund", explanationAnswer.acceptRefund);
                })

                .log("[" + EXPLANATION_ANSWER + "] Load travel")
                .inOut(GET_TRAVEL)

                .choice().when(simple("${body} == null"))
                    .process(e -> e.getIn().setBody(null))
                    .to(NOT_FOUND)
                    .stop()
                .end()

                .log("[" + EXPLANATION_ANSWER + "] Taking a decision to refund or not..")
                .choice()
                .when(simple("${header.acceptRefund} == true"))
                    .log("[" + EXPLANATION_ANSWER + "] Refund accepted by manager")
                    .inOut(Endpoints.ACCEPT_REFUND)
                .otherwise()
                    .log("[" + EXPLANATION_ANSWER + "] Refund refused by manager")
                    .inOut(Endpoints.REFUSE_REFUND)
                .end()

                .marshal().json(JsonLibrary.Jackson)
        ;
    }
}
