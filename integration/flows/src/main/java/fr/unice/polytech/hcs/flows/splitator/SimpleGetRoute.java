package fr.unice.polytech.hcs.flows.splitator;


import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.DataFormatDefinition;

import java.io.Serializable;
import java.util.Map;


public abstract class SimpleGetRoute<In extends Serializable, Out extends Serializable> extends RouteBuilder {

    // TODO dynamic endpoint
    private final String routeUri;
    private final String endpoint;
    private final DataFormatDefinition dataFormatDef;
    private final Converter<In, Object> genericReqConverter;
    private final Converter<Map, Out> specificResConverter;
    private final String routeId;
    private final String routeDescription;

    public SimpleGetRoute(String routeUri,
                          String endpoint,
                          DataFormatDefinition dataFormatDef,
                          Converter<In, Object> genericReqConverter,
                          Converter<Map, Out> specificResConverter,
                          String routeId,
                          String routeDescription) {
        this.routeUri = routeUri;
        this.endpoint = endpoint;
        this.dataFormatDef = dataFormatDef;
        this.genericReqConverter = genericReqConverter;
        this.specificResConverter = specificResConverter;
        this.routeId = routeId;
        this.routeDescription = routeDescription;
    }

    @Override
    public void configure() throws Exception {
        from(routeUri)
                .routeId(routeId)
                .routeDescription(routeDescription)

                .log("Create specific request from generic request")
                .process(e -> e.getIn().setBody(genericReqConverter.convert((In) e.getIn().getBody())))

                .log("Setting up request header")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))

                .log("Marshalling body ") // TODO make dynamic url
                .marshal(dataFormatDef)

                .log("Sending to endpoint")
                .inOut(endpoint)
                .log("Received specific request result")

                .log("Unmarshalling response")
                .unmarshal(dataFormatDef)
                .log("Converting to generic response")
                .process(e -> e.getIn().setBody(specificResConverter.convert((Map) e.getIn().getBody())))
        ;
    }
}
