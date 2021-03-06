package com.redhat.fuse.boosters.rest.http;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A simple Camel REST DSL route that implements the greetings service.
 * 
 */
@Component
public class CamelRouter extends RouteBuilder {

    @Autowired
    private CamelContext camelContext;

    @Override
    public void configure() throws Exception {

        KafkaComponent kafka = new KafkaComponent();
        kafka.setBrokers("uscis-kafka-bootstrap.amq-streams.svc:9092");
        camelContext.addComponent("kafka", kafka);

        // @formatter:off
        restConfiguration()
                .apiContextPath("/api-doc")
                .apiProperty("api.title", "Greeting REST API")
                .apiProperty("api.version", "1.0")
                .apiProperty("cors", "true")
                .apiProperty("base.path", "camel/")
                .apiProperty("api.path", "/")
                .apiProperty("host", "")
//                .apiProperty("schemes", "")
                .apiContextRouteId("doc-api")
            .component("servlet")
            .bindingMode(RestBindingMode.json);

        rest("/kafka-rest-proxy/pre-adjudication").description("")
            .post().consumes("application/json")
                .to("direct:pre-adjudication-kafka-proxy");
        
        //rest("/greetings/").description("Greeting to {name}")
        //    .get("/{name}").outType(Greetings.class)
        //        .route().routeId("greeting-api")
        //        .to("direct:greetingsImpl");

        //from("direct:greetingsImpl").description("Greetings REST service implementation route")
        //    .streamCaching()
        //    .to("bean:greetingsService?method=getGreetings");

        from("direct:pre-adjudication-kafka-proxy").routeId("kafka_rest_proxy").description("Places the pre-adjudication form message on a kafka topic")
            .log("${body}")
            .to("kafka:eprocessing");

        from("kafka:eprocessing?groupId=pcs").routeId("pcs_client")
            .log("${body}");

        from("kafka:eprocessing?groupId=pdf_cms").routeId("pdf_cms_client")
                .log("PDF/CMS Client received an event from the eprocessing kafka topic: ${body}");

        from("kafka:eprocessing?groupId=other_clients").routeId("other_clients")
                .log("Generic (Other) client received an event from the kafka topic: ${body}");
        // @formatter:on
    }

}