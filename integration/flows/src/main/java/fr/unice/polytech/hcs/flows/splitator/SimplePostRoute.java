package fr.unice.polytech.hcs.flows.splitator;


import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.DataFormatDefinition;

import java.io.Serializable;
import java.util.Map;


public abstract class SimplePostRoute<In extends Serializable, Out extends Serializable> extends RouteBuilder {

    private final String routeUri;
    private final String endpoint;
    private final DataFormatDefinition dataFormatDefIn;
    private final DataFormatDefinition dataFormatDefOut;
    private final Converter<In, Object> genericRecConverter;
    private final Converter<Map, Out> specificResConverter;
    private final String routeId;
    private final String routeDescription;

    public SimplePostRoute(String routeUri,
                           String endpoint,
                           DataFormatDefinition dataFormatDefIn,
                           DataFormatDefinition dataFormatDefOut,
                           Converter<In, Object> genericRecConverter,
                           Converter<Map, Out> specificResConverter,
                           String routeId,
                           String routeDescription) {
        this.routeUri = routeUri;
        this.endpoint = endpoint;
        this.dataFormatDefIn = dataFormatDefIn;
        this.dataFormatDefOut = dataFormatDefOut;
        this.genericRecConverter = genericRecConverter;
        this.specificResConverter = specificResConverter;
        this.routeId = routeId;
        this.routeDescription = routeDescription;
    }

    public SimplePostRoute(String routeUri,
                           String endpoint,
                           DataFormatDefinition dataFormatDefIn,
                           Converter<In, Object> genericRecConverter,
                           Converter<Map, Out> specificResConverter,
                           String routeId,
                           String routeDescription) {
        this(routeUri, endpoint,
                dataFormatDefIn, dataFormatDefIn,
                genericRecConverter, specificResConverter,
                routeId, routeDescription);
    }

    @Override
    public void configure() throws Exception {
        from(routeUri)
                .routeId(routeId)
                .routeDescription(routeDescription)

                .log("Creating specific request from generic request")
                .process(e -> e.getIn().setBody(genericRecConverter.convert((In) e.getIn().getBody())))

                .log("Setting up request header")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("application/json"))

                .log("Marshalling body")
                .marshal(dataFormatDefIn)

                .log("Sending to endpoint")
                .inOut(endpoint)
                .log("Received specific request result")

                .log("Unmarshalling response")
                .unmarshal(dataFormatDefOut)
                .log("Converting to generic response")
                .process(e -> e.getIn().setBody(specificResConverter.convert((Map) e.getIn().getBody())))
        ;
    }
}
